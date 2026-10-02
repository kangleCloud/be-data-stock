package com.vita.stockmonitor.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.stockmonitor.dto.*;
import com.vita.stockmonitor.entity.StockMonitorConfig;
import com.vita.stockmonitor.entity.StockMonitorProfile;
import com.vita.stockmonitor.entity.StockSymbolDictionary;
import com.vita.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.StockMonitorRedisLock;
import com.vita.stockmonitor.service.StockMonitorService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 系统级监控清单及匿名只读大屏。 */
@Service
public class StockMonitorServiceImpl implements StockMonitorService {
    private static final String ENABLED_KEY = "stock:monitor:v1:enabled";
    private static final String CONFIG_LOCK = "stock:monitor:v1:config:lock";
    private static final String LAST_TRADE_DATE_KEY = "stock:monitor:v1:lastTradeDate";
    private static final String STATE_ID_KEY = "stock:monitor:v1:state-id";
    private static final String UPDATES_CHANNEL = "stock:monitor:v1:updates";
    private static final String MARKET_SNAPSHOT_KEY = "stock:market:v1:snapshot";
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int MAX_STOCKS = 10;
    private static final Set<String> QUOTE_STATUSES = Set.of("FRESH", "STALE", "ERROR");
    private static final DefaultRedisScript<Long> ENABLED_RESYNC_SCRIPT = new DefaultRedisScript<>(
            "local old=redis.call('GET',KEYS[2]); "
                    + "redis.call('SET',KEYS[1],ARGV[1]); redis.call('SET',KEYS[2],ARGV[2]); "
                    + "redis.call('PUBLISH',KEYS[3],cjson.encode({baseStateId=old or cjson.null,"
                    + "stateId=ARGV[2],changedSymbols={},resync=true})); return 1", Long.class);
    private static final DefaultRedisScript<Long> RESYNC_SCRIPT = new DefaultRedisScript<>(
            "local old=redis.call('GET',KEYS[1]); redis.call('SET',KEYS[1],ARGV[1]); "
                    + "redis.call('PUBLISH',KEYS[2],cjson.encode({baseStateId=old or cjson.null,"
                    + "stateId=ARGV[1],changedSymbols={},resync=true})); return 1", Long.class);

    private final StockSymbolDictionaryMapper dictionaryMapper;
    private final StockMonitorConfigMapper configMapper;
    private final StockMonitorProfileMapper profileMapper;
    private final StringRedisTemplate redisTemplate;
    private final StockMonitorRedisLock redisLock;
    private final TransactionTemplate transactions;
    private final ObjectMapper objectMapper;
    private final boolean xqEnabled;

    public StockMonitorServiceImpl(StockSymbolDictionaryMapper dictionaryMapper,
                                   StockMonitorConfigMapper configMapper,
                                   StockMonitorProfileMapper profileMapper,
                                   StringRedisTemplate redisTemplate,
                                   StockMonitorRedisLock redisLock,
                                   TransactionTemplate transactions,
                                   ObjectMapper objectMapper,
                                   StockMonitorProperty property) {
        this.dictionaryMapper = dictionaryMapper;
        this.configMapper = configMapper;
        this.profileMapper = profileMapper;
        this.redisTemplate = redisTemplate;
        this.redisLock = redisLock;
        this.transactions = transactions;
        this.objectMapper = objectMapper;
        this.xqEnabled = property.isXqEnabled();
    }

    @Override
    public PageResponse<StockMonitorDtos.AdminStock> pageMonitor(StockMonitorPageQuery request) {
        StockMonitorPageQuery queryRequest = request == null ? new StockMonitorPageQuery() : request;
        LambdaQueryWrapperX<StockMonitorConfig> query = new LambdaQueryWrapperX<>();
        query.eq(queryRequest.getEnabled() != null, StockMonitorConfig::getEnabled, queryRequest.getEnabled());
        if (hasText(queryRequest.getKeyword())) {
            String pattern = "%" + queryRequest.getKeyword().trim() + "%";
            query.apply("EXISTS (SELECT 1 FROM stock_symbol_dictionary d WHERE d.is_deleted = 0 "
                            + "AND d.symbol = stock_monitor_config.symbol "
                            + "AND (d.code LIKE {0} OR d.name LIKE {1}))", pattern, pattern);
        }
        query.orderByDesc(StockMonitorConfig::getEnabled)
                .orderByAsc(StockMonitorConfig::getSortOrder)
                .orderByAsc(StockMonitorConfig::getSymbol);
        PageResponse<StockMonitorConfig> page = configMapper.selectPage(queryRequest, query);
        List<String> symbols = page.getList().stream().map(StockMonitorConfig::getSymbol).toList();
        Map<String, StockSymbolDictionary> dictionary = dictionaryBySymbol(symbols);
        Map<String, StockMonitorProfile> profiles = profileBySymbol(symbols);
        List<StockMonitorDtos.AdminStock> rows = page.getList().stream().map(config -> {
            StockSymbolDictionary item = dictionary.get(config.getSymbol());
            String symbol = config.getSymbol();
            return new StockMonitorDtos.AdminStock(symbol, item == null ? null : item.getCode(),
                    item == null ? null : item.getName(), item == null ? null : item.getMarket(),
                    Boolean.TRUE.equals(config.getEnabled()), config.getSortOrder(),
                    profileView(profiles.get(symbol)));
        }).toList();
        return new PageResponse<>(rows, page.getTotal());
    }

    @Override
    public PageResponse<StockMonitorDtos.DictionaryItem> pageDictionary(StockDictionaryPageQuery request) {
        StockDictionaryPageQuery queryRequest = request == null ? new StockDictionaryPageQuery() : request;
        LambdaQueryWrapperX<StockSymbolDictionary> query = new LambdaQueryWrapperX<>();
        if (hasText(queryRequest.getKeyword())) {
            String keyword = queryRequest.getKeyword().trim();
            query.and(w -> w.like(StockSymbolDictionary::getCode, keyword)
                    .or().like(StockSymbolDictionary::getName, keyword));
        }
        if (hasText(queryRequest.getMarket())) {
            query.eq(StockSymbolDictionary::getMarket, queryRequest.getMarket().trim().toUpperCase(java.util.Locale.ROOT));
        }
        query.orderByAsc(StockSymbolDictionary::getSymbol);
        PageResponse<StockSymbolDictionary> page = dictionaryMapper.selectPage(queryRequest, query);
        return new PageResponse<>(page.getList().stream().map(this::dictionaryItem).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockMonitorDtos.DictionaryItem addDictionaryStock(StockDictionaryCreateDto request) {
        if (request == null || request.market() == null || request.code() == null || request.name() == null
                || !request.market().matches("SH|SZ|BJ") || !request.code().matches("[0-9]{6}")
                || request.name().isBlank() || request.name().trim().length() > 100) {
            throw badRequest("股票字典参数不合法");
        }
        String symbol = request.market() + request.code();
        if (dictionaryMapper.selectOne(StockSymbolDictionary::getSymbol, symbol) != null) {
            throw badRequest("股票已存在于字典中");
        }
        StockSymbolDictionary stock = new StockSymbolDictionary();
        stock.setSymbol(symbol);
        stock.setCode(request.code());
        stock.setMarket(request.market());
        stock.setName(request.name().trim());
        try {
            dictionaryMapper.insert(stock);
        } catch (DuplicateKeyException exception) {
            // 并发补录时由唯一索引最终保证同一交易所代码只有一条记录。
            throw badRequest("股票已存在于字典中");
        }
        return dictionaryItem(stock);
    }

    @Override
    public PageResponse<StockMonitorDtos.ProfileStock> pageProfile(StockProfilePageQuery request) {
        StockProfilePageQuery queryRequest = request == null ? new StockProfilePageQuery() : request;
        LambdaQueryWrapperX<StockMonitorProfile> query = new LambdaQueryWrapperX<>();
        if (hasText(queryRequest.getKeyword())) {
            String pattern = "%" + queryRequest.getKeyword().trim() + "%";
            query.apply("EXISTS (SELECT 1 FROM stock_symbol_dictionary d WHERE d.is_deleted = 0 "
                            + "AND d.symbol = stock_monitor_profile.symbol "
                            + "AND (d.code LIKE {0} OR d.name LIKE {1}))", pattern, pattern);
        }
        if (hasText(queryRequest.getIndustry())) {
            query.like(StockMonitorProfile::getIndustry, queryRequest.getIndustry().trim());
        }
        query.orderByAsc(StockMonitorProfile::getSymbol);
        PageResponse<StockMonitorProfile> page = profileMapper.selectPage(queryRequest, query);
        Map<String, StockSymbolDictionary> dictionary = dictionaryBySymbol(
                page.getList().stream().map(StockMonitorProfile::getSymbol).toList());
        List<StockMonitorDtos.ProfileStock> rows = page.getList().stream().map(profile -> {
            StockSymbolDictionary item = dictionary.get(profile.getSymbol());
            return new StockMonitorDtos.ProfileStock(profile.getSymbol(),
                    item == null ? null : item.getCode(), item == null ? null : item.getName(),
                    item == null ? null : item.getMarket(), profile.getIndustry(), profile.getListingDate(),
                    profile.getMarketCap(), profile.getUpdatedAt());
        }).toList();
        return new PageResponse<>(rows, page.getTotal());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public List<StockMonitorDtos.DictionaryItem> searchDictionary(String keyword, int limit) {
        if (limit < 1 || limit > 50) {
            throw badRequest("limit 必须在 1 到 50 之间");
        }
        LambdaQueryWrapperX<StockSymbolDictionary> query = new LambdaQueryWrapperX<>();
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            query.and(w -> w.like(StockSymbolDictionary::getCode, value)
                    .or().like(StockSymbolDictionary::getName, value)
                    .or().like(StockSymbolDictionary::getSymbol, value));
        }
        query.orderByAsc(StockSymbolDictionary::getSymbol).last("LIMIT " + limit);
        return dictionaryMapper.selectList(query).stream().map(this::dictionaryItem).toList();
    }

    @Override
    public List<StockMonitorDtos.AdminStock> listAdmin() {
        List<StockMonitorConfig> configs = configs(true);
        if (configs.isEmpty()) {
            return List.of();
        }
        List<String> symbols = configs.stream().map(StockMonitorConfig::getSymbol).toList();
        Map<String, StockSymbolDictionary> dictionary = dictionaryBySymbol(symbols);
        Map<String, StockMonitorProfile> profiles = profileBySymbol(symbols);
        List<StockMonitorDtos.AdminStock> rows = new ArrayList<>();
        for (StockMonitorConfig config : configs) {
            StockSymbolDictionary item = dictionary.get(config.getSymbol());
            if (item != null) {
                rows.add(new StockMonitorDtos.AdminStock(item.getSymbol(), item.getCode(), item.getName(),
                        item.getMarket(), Boolean.TRUE.equals(config.getEnabled()), config.getSortOrder(),
                        profileView(profiles.get(item.getSymbol()))));
            }
        }
        return rows;
    }

    @Override
    public void setEnabled(String symbol, Boolean enabled) {
        validateSymbol(symbol);
        if (enabled == null) {
            throw badRequest("enabled 不能为空");
        }
        withConfigLock(() -> {
            transactions.executeWithoutResult(status -> {
                StockSymbolDictionary item = dictionaryMapper.selectOne(StockSymbolDictionary::getSymbol, symbol);
                if (item == null) {
                    throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "股票不在交易所字典中");
                }
                StockMonitorConfig config = configMapper.selectOne(StockMonitorConfig::getSymbol, symbol);
                if (Boolean.TRUE.equals(enabled)) {
                    List<StockMonitorConfig> active = configs(true);
                    if ((config == null || !Boolean.TRUE.equals(config.getEnabled())) && active.size() >= MAX_STOCKS) {
                        throw badRequest("最多只能启用 10 只股票");
                    }
                    if (config == null) {
                        config = new StockMonitorConfig();
                        config.setSymbol(symbol);
                        config.setSortOrder(active.stream().mapToInt(StockMonitorConfig::getSortOrder)
                                .max().orElse(0) + 1);
                        config.setEnabled(true);
                        configMapper.insert(config);
                    } else if (!Boolean.TRUE.equals(config.getEnabled())) {
                        config.setEnabled(true);
                        configMapper.updateById(config);
                    }
                } else if (config != null && Boolean.TRUE.equals(config.getEnabled())) {
                    config.setEnabled(false);
                    configMapper.updateById(config);
                    profileMapper.deleteBySymbol(symbol);
                }
            });
            rebuildEnabledCache();
        });
    }

    @Override
    public void sort(List<String> symbols) {
        if (symbols == null || symbols.size() > MAX_STOCKS || new HashSet<>(symbols).size() != symbols.size()) {
            throw badRequest("排序股票集合不合法");
        }
        symbols.forEach(this::validateSymbol);
        withConfigLock(() -> {
            transactions.executeWithoutResult(status -> {
                List<StockMonitorConfig> active = configs(true);
                Set<String> current = active.stream().map(StockMonitorConfig::getSymbol).collect(Collectors.toSet());
                if (!current.equals(new HashSet<>(symbols))) {
                    throw badRequest("排序必须包含当前全部启用股票");
                }
                Map<String, StockMonitorConfig> bySymbol = active.stream().collect(Collectors.toMap(
                        StockMonitorConfig::getSymbol, Function.identity()));
                for (int i = 0; i < symbols.size(); i++) {
                    StockMonitorConfig config = bySymbol.get(symbols.get(i));
                    config.setSortOrder(i + 1);
                    configMapper.updateById(config);
                }
            });
            rebuildEnabledCache();
        });
    }

    @Override
    public List<String> enabledSymbols() {
        return configs(true).stream().map(StockMonitorConfig::getSymbol).toList();
    }

    @Override
    public void rebuildEnabledCache() {
        List<StockMonitorConfig> active = configs(true);
        if (active.size() > MAX_STOCKS) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "启用股票超过 10 只");
        }
        Map<String, StockSymbolDictionary> dictionary = dictionaryBySymbol(
                active.stream().map(StockMonitorConfig::getSymbol).toList());
        List<StockMonitorDtos.DictionaryItem> enabled = active.stream()
                .map(config -> dictionary.get(config.getSymbol()))
                .filter(item -> item != null)
                .map(this::dictionaryItem).toList();
        if (enabled.size() != active.size()) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "启用股票缺少字典资料");
        }
        try {
            Long result = redisTemplate.execute(ENABLED_RESYNC_SCRIPT,
                    List.of(ENABLED_KEY, STATE_ID_KEY, UPDATES_CHANNEL),
                    objectMapper.writeValueAsString(enabled), UUID.randomUUID().toString().replace("-", ""));
            if (!Long.valueOf(1).equals(result)) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "监控清单缓存发布失败");
            }
        } catch (JsonProcessingException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "监控清单缓存序列化失败");
        }
    }

    @Override
    public void publishResync() {
        Long result = redisTemplate.execute(RESYNC_SCRIPT, List.of(STATE_ID_KEY, UPDATES_CHANNEL),
                UUID.randomUUID().toString().replace("-", ""));
        if (!Long.valueOf(1).equals(result)) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "个股状态版本发布失败");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void prewarmEnabledCache() {
        // 启动只读 MySQL 配置并重建缓存，不发起交易所或雪球请求。
        rebuildEnabledCache();
    }

    @Override
    public StockMonitorDtos.Dashboard dashboard() {
        String before = readStateId();
        StockMonitorDtos.Dashboard dashboard = buildDashboard(before);
        if (!Objects.equals(before, readStateId())) {
            String retryId = readStateId();
            dashboard = buildDashboard(retryId);
            if (!Objects.equals(retryId, readStateId())) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "个股数据正在更新，请重试");
            }
        }
        return dashboard;
    }

    private StockMonitorDtos.Dashboard buildDashboard(String stateId) {
        List<StockMonitorDtos.DictionaryItem> enabled = readEnabledCache();
        String tradeDate = xqEnabled ? validTradeDate(redisTemplate.opsForValue().get(LAST_TRADE_DATE_KEY)) : null;
        String fundDate = xqEnabled ? readMarketFundDate() : null;
        String today = LocalDate.now(SHANGHAI).toString();
        Map<String, StockMonitorProfile> profiles = xqEnabled
                ? profileBySymbol(enabled.stream().map(StockMonitorDtos.DictionaryItem::symbol).toList())
                : Map.of();
        List<StockMonitorDtos.Stock> stocks = new ArrayList<>();
        for (int i = 0; i < enabled.size(); i++) {
            StockMonitorDtos.DictionaryItem item = enabled.get(i);
            StockMonitorDtos.Quote quote = xqEnabled ? readQuote(item.symbol(), tradeDate) : emptyQuote("DISABLED");
            String effectiveDate = quote.tradeDate();
            List<StockMonitorDtos.SeriesPoint> latestPriceSeries = xqEnabled && tradeDate != null
                    ? readSeries(tradeDate, item.symbol()) : List.of();
            if (!latestPriceSeries.isEmpty() && (effectiveDate == null || tradeDate.compareTo(effectiveDate) > 0)) {
                effectiveDate = tradeDate;
                if (quote.tradeDate() != null) {
                    quote = emptyQuote("ERROR");
                }
            }
            List<StockMonitorDtos.SeriesPoint> series = effectiveDate != null && effectiveDate.equals(tradeDate)
                    ? latestPriceSeries : List.of();
            if (effectiveDate == null && fundDate != null
                    && !readFundSeries(fundDate, item.symbol()).isEmpty()) {
                effectiveDate = fundDate;
            }
            if (effectiveDate == null && tradeDate != null && !tradeDate.equals(fundDate)
                    && !readFundSeries(tradeDate, item.symbol()).isEmpty()) {
                effectiveDate = tradeDate;
            }
            if (effectiveDate != null && series.isEmpty()) {
                series = readSeries(effectiveDate, item.symbol());
            }
            List<StockMonitorDtos.FundPoint> fundSeries = effectiveDate == null
                    ? List.of() : readFundSeries(effectiveDate, item.symbol());
            boolean closeConfirmed = closeConfirmed(quote);
            String dataStatus = !xqEnabled ? "DISABLED" : effectiveDate == null ? "NO_DATA"
                    : !today.equals(effectiveDate) ? "HISTORICAL"
                    : "FRESH".equals(quote.status()) || closeConfirmed ? "CURRENT" : "DELAYED";
            stocks.add(new StockMonitorDtos.Stock(item.symbol(), item.code(), item.name(), item.market(),
                    i + 1, profileView(profiles.get(item.symbol())), quote, series,
                    effectiveDate, dataStatus, closeConfirmed, fundSeries));
        }
        return new StockMonitorDtos.Dashboard(1, stateId, xqEnabled, tradeDate, stocks);
    }

    private String readStateId() {
        String value = redisTemplate.opsForValue().get(STATE_ID_KEY);
        if (value != null && !value.matches("[0-9a-f]{32}")) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "个股状态版本格式错误");
        }
        return value;
    }

    private String readMarketFundDate() {
        String raw = redisTemplate.opsForValue().get(MARKET_SNAPSHOT_KEY);
        if (raw == null) {
            return null;
        }
        try {
            JsonNode module = objectMapper.readTree(raw).path("modules").path("marketFundFlow");
            return module.path("data").isObject() ? validTradeDate(module.path("tradeDate").asText(null)) : null;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private boolean closeConfirmed(StockMonitorDtos.Quote quote) {
        if (quote.sourceTime() == null || quote.tradeDate() == null) {
            return false;
        }
        OffsetDateTime source = OffsetDateTime.parse(quote.sourceTime());
        return quote.tradeDate().equals(source.atZoneSameInstant(SHANGHAI).toLocalDate().toString())
                && source.atZoneSameInstant(SHANGHAI).toLocalTime().isAfter(LocalTime.of(15, 0));
    }

    private List<StockMonitorDtos.FundPoint> readFundSeries(String tradeDate, String symbol) {
        String raw = redisTemplate.opsForValue().get("stock:monitor:v1:fund-series:" + tradeDate + ":" + symbol);
        if (raw == null) {
            return List.of();
        }
        try {
            JsonNode json = objectMapper.readTree(raw);
            if (!json.isArray()) {
                return List.of();
            }
            List<StockMonitorDtos.FundPoint> points = new ArrayList<>();
            OffsetDateTime previous = null;
            for (JsonNode point : json) {
                String time = point.path("collectedAt").asText(null);
                BigDecimal inflow = number(point.get("inflow"));
                BigDecimal outflow = number(point.get("outflow"));
                BigDecimal netAmount = number(point.get("netAmount"));
                if (!validOffsetTime(time) || !tradeDate.equals(OffsetDateTime.parse(time)
                        .atZoneSameInstant(SHANGHAI).toLocalDate().toString())
                        || inflow == null || outflow == null || netAmount == null
                        || netAmount.compareTo(inflow.subtract(outflow)) != 0) {
                    return List.of();
                }
                OffsetDateTime collectedAt = OffsetDateTime.parse(time);
                if (previous != null && !collectedAt.isAfter(previous)) {
                    return List.of();
                }
                points.add(new StockMonitorDtos.FundPoint(time, inflow, outflow, netAmount));
                previous = collectedAt;
            }
            return points;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return List.of();
        }
    }

    private List<StockMonitorDtos.DictionaryItem> readEnabledCache() {
        String raw = redisTemplate.opsForValue().get(ENABLED_KEY);
        if (raw == null) {
            // 仅显式 [] 表示没有启用股票；键丢失代表缓存不可用，不能误报为空清单。
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "监控清单缓存不可用");
        }
        try {
            JsonNode json = objectMapper.readTree(raw);
            if (!json.isArray() || json.size() > MAX_STOCKS) {
                throw new IllegalArgumentException("enabled 清单格式错误");
            }
            List<StockMonitorDtos.DictionaryItem> items = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (JsonNode item : json) {
                String symbol = item.path("symbol").asText("");
                String code = item.path("code").asText("");
                String name = item.path("name").asText("");
                String market = item.path("market").asText("");
                if (!symbol.matches("^(SH|SZ|BJ)[0-9]{6}$") || !symbol.substring(2).equals(code)
                        || !symbol.substring(0, 2).equals(market) || name.isBlank()
                        || !seen.add(symbol)) {
                    throw new IllegalArgumentException("enabled 清单字段错误");
                }
                items.add(new StockMonitorDtos.DictionaryItem(symbol, code, name, market));
            }
            return items;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "监控清单缓存格式错误");
        }
    }

    private StockMonitorDtos.Quote readQuote(String symbol, String tradeDate) {
        String raw = redisTemplate.opsForValue().get("stock:monitor:v1:quote:" + symbol);
        if (raw == null) {
            return emptyQuote("ERROR");
        }
        try {
            JsonNode json = objectMapper.readTree(raw);
            if (json.path("schemaVersion").asInt() != 1 || !symbol.equals(json.path("symbol").asText())
                    || !"XQ".equals(json.path("source").asText())
                    || !QUOTE_STATUSES.contains(json.path("status").asText())
                    || !validOffsetTime(json.path("sourceTime").asText(null))
                    || !validOffsetTime(json.path("collectedAt").asText(null))) {
                return emptyQuote("ERROR");
            }
            String date = validTradeDate(json.path("tradeDate").asText(null));
            if (date == null) {
                return emptyQuote("ERROR");
            }
            if (!date.equals(OffsetDateTime.parse(json.path("sourceTime").asText())
                    .atZoneSameInstant(SHANGHAI).toLocalDate().toString())) {
                return emptyQuote("ERROR");
            }
            if ("ERROR".equals(json.path("status").asText())) {
                return emptyQuote("ERROR");
            }
            String status = tradeDate == null || !tradeDate.equals(date)
                    ? "STALE" : json.path("status").asText();
            return new StockMonitorDtos.Quote("XQ", json.path("sourceTime").asText(),
                    json.path("collectedAt").asText(), date, quoteNumber(json.get("price")),
                    quoteNumber(json.get("changePercent")), quoteNumber(json.get("amount")),
                    quoteNumber(json.get("low")), quoteNumber(json.get("high")),
                    quoteNumber(json.get("open")), quoteNumber(json.get("limitUp")),
                    quoteNumber(json.get("limitDown")), quoteNumber(json.get("averagePrice")),
                    quoteNumber(json.get("volume")), quoteNumber(json.get("previousClose")), status);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return emptyQuote("ERROR");
        }
    }

    private List<StockMonitorDtos.SeriesPoint> readSeries(String tradeDate, String symbol) {
        String raw = redisTemplate.opsForValue().get("stock:monitor:v1:series:" + tradeDate + ":" + symbol);
        if (raw == null) {
            return List.of();
        }
        try {
            JsonNode json = objectMapper.readTree(raw);
            if (!json.isArray()) {
                return List.of();
            }
            List<StockMonitorDtos.SeriesPoint> points = new ArrayList<>();
            String previous = null;
            for (JsonNode point : json) {
                String time = point.path("time").asText(null);
                BigDecimal price = number(point.get("price"));
                if (!validOffsetTime(time) || !tradeDate.equals(OffsetDateTime.parse(time)
                        .atZoneSameInstant(SHANGHAI).toLocalDate().toString())
                        || price == null || previous != null && previous.compareTo(time) >= 0) {
                    return List.of();
                }
                points.add(new StockMonitorDtos.SeriesPoint(time, price));
                previous = time;
            }
            return points;
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<StockMonitorConfig> configs(boolean enabledOnly) {
        LambdaQueryWrapperX<StockMonitorConfig> query = new LambdaQueryWrapperX<>();
        query.eq(enabledOnly, StockMonitorConfig::getEnabled, true)
                .orderByAsc(StockMonitorConfig::getSortOrder)
                .orderByAsc(StockMonitorConfig::getSymbol);
        return configMapper.selectList(query);
    }

    private Map<String, StockSymbolDictionary> dictionaryBySymbol(List<String> symbols) {
        if (symbols.isEmpty()) {
            return Map.of();
        }
        return dictionaryMapper.selectList(StockSymbolDictionary::getSymbol, symbols).stream()
                .collect(Collectors.toMap(StockSymbolDictionary::getSymbol, Function.identity()));
    }

    private Map<String, StockMonitorProfile> profileBySymbol(List<String> symbols) {
        if (symbols.isEmpty()) {
            return Map.of();
        }
        return profileMapper.selectList(StockMonitorProfile::getSymbol, symbols).stream()
                .collect(Collectors.toMap(StockMonitorProfile::getSymbol, Function.identity()));
    }

    private StockMonitorDtos.DictionaryItem dictionaryItem(StockSymbolDictionary item) {
        return new StockMonitorDtos.DictionaryItem(item.getSymbol(), item.getCode(), item.getName(), item.getMarket());
    }

    private StockMonitorDtos.Profile profileView(StockMonitorProfile profile) {
        return profile == null ? new StockMonitorDtos.Profile(null, null, null, null)
                : new StockMonitorDtos.Profile(profile.getIndustry(), profile.getListingDate(),
                profile.getMarketCap(), profile.getUpdatedAt());
    }

    private StockMonitorDtos.Quote emptyQuote(String status) {
        return new StockMonitorDtos.Quote("XQ", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, status);
    }

    private BigDecimal quoteNumber(JsonNode node) {
        if (node == null || node.isNull()) {
            return null; // 旧版 V1 报价没有扩展字段，缺失数值仍可读取。
        }
        if (!node.isNumber() || node.isFloatingPointNumber() && !Double.isFinite(node.doubleValue())) {
            throw new IllegalArgumentException("报价数值无效");
        }
        return node.decimalValue();
    }

    private BigDecimal number(JsonNode node) {
        return node != null && node.isNumber()
                && (!node.isFloatingPointNumber() || Double.isFinite(node.doubleValue()))
                ? node.decimalValue() : null;
    }

    private String validTradeDate(String date) {
        try {
            return date != null && LocalDate.parse(date).toString().equals(date) ? date : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean validOffsetTime(String time) {
        try {
            return time != null && OffsetDateTime.parse(time).getOffset().equals(ZoneOffset.ofHours(8));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || !symbol.matches("^(SH|SZ|BJ)[0-9]{6}$")) {
            throw badRequest("股票标识格式错误");
        }
    }

    private ServiceException badRequest(String message) {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    private void withConfigLock(Runnable action) {
        String token = redisLock.acquire(CONFIG_LOCK, Duration.ofSeconds(30));
        if (token == null) {
            throw badRequest("监控清单正在修改，请稍后重试");
        }
        try {
            action.run();
        } finally {
            redisLock.release(CONFIG_LOCK, token);
        }
    }
}
