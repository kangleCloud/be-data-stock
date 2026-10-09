package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.marketdata.constant.MarketDataConstants;
import com.vita.marketdata.etfmonitor.constant.EtfMonitorConstants;
import com.vita.marketdata.etfmonitor.dto.EtfDictionaryPageQuery;
import com.vita.marketdata.etfmonitor.dto.EtfMonitorDtos;
import com.vita.marketdata.etfmonitor.dto.EtfProfilePageQuery;
import com.vita.marketdata.etfmonitor.entity.EtfAssetAllocationReport;
import com.vita.marketdata.etfmonitor.entity.EtfMonitorConfig;
import com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile;
import com.vita.marketdata.etfmonitor.entity.EtfSymbolDictionary;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorConfigMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfSymbolDictionaryMapper;
import com.vita.marketdata.support.MarketDataRedisLock;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** ETF 独立的最多十只启用清单。 */
@Service
public class EtfMonitorConfigService {
    private static final DefaultRedisScript<Long> PUBLISH_SCRIPT = new DefaultRedisScript<>(
            "local old=redis.call('GET',KEYS[2]); redis.call('SET',KEYS[1],ARGV[1]); "
                    + "redis.call('SET',KEYS[2],ARGV[2]); redis.call('PUBLISH',KEYS[3],"
                    + "cjson.encode({baseStateId=old or cjson.null,stateId=ARGV[2],changedSymbols={},resync=true})); "
                    + "return 1", Long.class);

    private final EtfSymbolDictionaryMapper dictionaryMapper;
    private final EtfMonitorConfigMapper configMapper;
    private final EtfMonitorProfileMapper profileMapper;
    private final EtfAssetAllocationReportMapper allocationMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final MarketDataRedisLock redisLock;
    private final TransactionTemplate transactions;

    public EtfMonitorConfigService(EtfSymbolDictionaryMapper dictionaryMapper,
                                   EtfMonitorConfigMapper configMapper,
                                   EtfMonitorProfileMapper profileMapper,
                                   EtfAssetAllocationReportMapper allocationMapper,
                                   StringRedisTemplate redis, ObjectMapper json,
                                   MarketDataRedisLock redisLock, TransactionTemplate transactions) {
        this.dictionaryMapper = dictionaryMapper;
        this.configMapper = configMapper;
        this.profileMapper = profileMapper;
        this.allocationMapper = allocationMapper;
        this.redis = redis;
        this.json = json;
        this.redisLock = redisLock;
        this.transactions = transactions;
    }

    public PageResponse<EtfSymbolDictionary> pageDictionary(EtfDictionaryPageQuery request) {
        LambdaQueryWrapperX<EtfSymbolDictionary> query = new LambdaQueryWrapperX<>();
        if (hasText(request.getKeyword())) {
            query.and(w -> w.like(EtfSymbolDictionary::getCode, request.getKeyword().trim())
                    .or().like(EtfSymbolDictionary::getName, request.getKeyword().trim()));
        }
        query.eq(hasText(request.getMarket()), EtfSymbolDictionary::getMarket, request.getMarket());
        query.eq(hasText(request.getEtfType()), EtfSymbolDictionary::getEtfType, request.getEtfType());
        query.orderByAsc(EtfSymbolDictionary::getSymbol);
        return dictionaryMapper.selectPage(request, query);
    }

    public PageResponse<EtfMonitorDtos.ProfileRow> pageProfile(EtfProfilePageQuery request) {
        LambdaQueryWrapperX<EtfMonitorProfile> query = new LambdaQueryWrapperX<>();
        if (hasText(request.getKeyword())) {
            String pattern = "%" + request.getKeyword().trim() + "%";
            query.apply("EXISTS (SELECT 1 FROM etf_symbol_dictionary d WHERE d.is_deleted=0 "
                    + "AND d.symbol=etf_monitor_profile.symbol AND (d.code LIKE {0} OR d.name LIKE {1}))",
                    pattern, pattern);
        }
        query.eq(hasText(request.getFundType()), EtfMonitorProfile::getFundType, request.getFundType());
        query.eq(hasText(request.getTrackingIndexCode()), EtfMonitorProfile::getTrackingIndexCode,
                request.getTrackingIndexCode());
        query.orderByAsc(EtfMonitorProfile::getSymbol);
        PageResponse<EtfMonitorProfile> page = profileMapper.selectPage(request, query);
        List<String> symbols = page.getList().stream().map(EtfMonitorProfile::getSymbol).toList();
        Map<String, EtfSymbolDictionary> dictionary = symbols.isEmpty() ? Map.of()
                : dictionaryMapper.selectList(EtfSymbolDictionary::getSymbol, symbols).stream()
                .collect(Collectors.toMap(EtfSymbolDictionary::getSymbol, Function.identity()));
        return new PageResponse<>(page.getList().stream()
                .map(profile -> profileRow(profile, dictionary.get(profile.getSymbol()))).toList(), page.getTotal());
    }

    private EtfMonitorDtos.ProfileRow profileRow(EtfMonitorProfile profile, EtfSymbolDictionary item) {
        // 旧交易所资料不再展示；部署升级会清空非 THS 行，读取时也按无资料处理。
        if (!"THS".equals(profile.getSource())) {
            EtfMonitorProfile empty = new EtfMonitorProfile();
            empty.setSymbol(profile.getSymbol());
            profile = empty;
        }
        return new EtfMonitorDtos.ProfileRow(profile.getSymbol(),
                item == null ? null : item.getCode(), item == null ? null : item.getName(),
                item == null ? null : item.getMarket(), profile.getExchange(),
                item == null ? profile.getEtfType() : item.getEtfType(),
                profile.getListingStatus(), profile.getListingDate(), profile.getManager(),
                profile.getCustodian(), profile.getShareCount(), profile.getShareDate(),
                profile.getTrackingIndexCode(), profile.getTrackingIndexName(),
                profile.getProfileUpdatedAt() == null ? null : profile.getProfileUpdatedAt()
                        .atZone(MarketDataConstants.SHANGHAI).toOffsetDateTime().toString(),
                profile.getFullName(), profile.getFundType(), profile.getInvestmentType(),
                profile.getFundManager(), profile.getEstablishedDate(), profile.getPerformanceBenchmark(),
                profile.getSource());
    }

    public EtfMonitorDtos.ProfileDetail detail(String symbol) {
        validateSymbol(symbol);
        EtfSymbolDictionary dictionary = dictionaryMapper.selectOne(EtfSymbolDictionary::getSymbol, symbol);
        if (dictionary == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "ETF 字典中没有该代码");
        }
        EtfMonitorProfile profile = profileMapper.selectOne(EtfMonitorProfile::getSymbol, symbol);
        // 管理员可以查看已有报告；总闸仅限制公开展示和新的雪球采集，不删除历史资料。
        EtfAssetAllocationReport report = allocationMapper.selectOne(
                new LambdaQueryWrapperX<EtfAssetAllocationReport>()
                        .eq(EtfAssetAllocationReport::getSymbol, symbol)
                        .orderByDesc(EtfAssetAllocationReport::getRequestedReportPeriod).last("LIMIT 1"));
        Object allocation = null;
        if (report != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode categories = json.readTree(report.getCategoriesJson());
                if (!categories.isArray()) {
                    throw new IllegalArgumentException("资产配置类别格式错误");
                }
                com.fasterxml.jackson.databind.node.ObjectNode value = json.createObjectNode();
                value.put("requestedReportPeriod", report.getRequestedReportPeriod().toString());
                value.put("source", report.getSource());
                value.put("collectedAt", report.getCollectedAt()
                        .atZone(MarketDataConstants.SHANGHAI).toOffsetDateTime().toString());
                value.set("categories", categories);
                allocation = value;
            } catch (JsonProcessingException | IllegalArgumentException exception) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "资产配置报告格式错误");
            }
        }
        return new EtfMonitorDtos.ProfileDetail(dictionary, profile == null ? null : profileRow(profile, dictionary), allocation);
    }

    public List<EtfMonitorDtos.AdminEtf> list() {
        List<EtfMonitorConfig> active = active();
        if (active.isEmpty()) {
            return List.of();
        }
        Map<String, EtfSymbolDictionary> dictionary = dictionaryMapper.selectList(
                EtfSymbolDictionary::getSymbol, active.stream().map(EtfMonitorConfig::getSymbol).toList())
                .stream().collect(Collectors.toMap(EtfSymbolDictionary::getSymbol, Function.identity()));
        return active.stream().map(config -> {
            EtfSymbolDictionary item = dictionary.get(config.getSymbol());
            return new EtfMonitorDtos.AdminEtf(config.getSymbol(), item == null ? null : item.getCode(),
                    item == null ? null : item.getName(), item == null ? null : item.getMarket(),
                    true, config.getSortOrder());
        }).toList();
    }

    public void setEnabled(String symbol, Boolean enabled) {
        validateSymbol(symbol);
        if (enabled == null) {
            throw badRequest("enabled 不能为空");
        }
        withLock(() -> {
            transactions.executeWithoutResult(status -> {
                if (dictionaryMapper.selectOne(EtfSymbolDictionary::getSymbol, symbol) == null) {
                    throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "ETF 不在字典中");
                }
                EtfMonitorConfig config = configMapper.selectOne(EtfMonitorConfig::getSymbol, symbol);
                if (enabled && (config == null || !Boolean.TRUE.equals(config.getEnabled()))) {
                    List<EtfMonitorConfig> current = active();
                    if (current.size() >= MarketDataConstants.MAX_MONITORS) {
                        throw badRequest("最多只能启用 10 只 ETF");
                    }
                    if (config == null) {
                        config = new EtfMonitorConfig();
                        config.setSymbol(symbol);
                        config.setSortOrder(current.stream().mapToInt(EtfMonitorConfig::getSortOrder)
                                .max().orElse(0) + 1);
                        config.setEnabled(true);
                        configMapper.insert(config);
                    } else {
                        config.setEnabled(true);
                        configMapper.updateById(config);
                    }
                } else if (!enabled && config != null && Boolean.TRUE.equals(config.getEnabled())) {
                    config.setEnabled(false);
                    configMapper.updateById(config);
                }
            });
            rebuildEnabledCache();
        });
    }

    public void sort(List<String> symbols) {
        if (symbols == null || symbols.size() > MarketDataConstants.MAX_MONITORS || new HashSet<>(symbols).size() != symbols.size()) {
            throw badRequest("排序 ETF 集合不合法");
        }
        symbols.forEach(this::validateSymbol);
        withLock(() -> {
            transactions.executeWithoutResult(status -> {
                List<EtfMonitorConfig> current = active();
                if (!current.stream().map(EtfMonitorConfig::getSymbol).collect(Collectors.toSet())
                        .equals(new HashSet<>(symbols))) {
                    throw badRequest("排序必须包含当前全部启用 ETF");
                }
                Map<String, EtfMonitorConfig> bySymbol = current.stream()
                        .collect(Collectors.toMap(EtfMonitorConfig::getSymbol, Function.identity()));
                for (int i = 0; i < symbols.size(); i++) {
                    EtfMonitorConfig config = bySymbol.get(symbols.get(i));
                    config.setSortOrder(i + 1);
                    configMapper.updateById(config);
                }
            });
            rebuildEnabledCache();
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void prewarm() {
        rebuildEnabledCache();
    }

    public void rebuildEnabledCache() {
        List<EtfMonitorDtos.AdminEtf> active = list();
        if (active.size() > MarketDataConstants.MAX_MONITORS || active.stream().anyMatch(item -> item.code() == null
                || item.name() == null || item.market() == null)) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "ETF 启用清单数据不完整");
        }
        List<Map<String, String>> enabled = new ArrayList<>();
        for (EtfMonitorDtos.AdminEtf item : active) {
            enabled.add(Map.of("symbol", item.symbol(), "code", item.code(),
                    "name", item.name(), "market", item.market()));
        }
        try {
            Long result = redis.execute(PUBLISH_SCRIPT, List.of(EtfMonitorConstants.ENABLED_KEY, EtfMonitorConstants.STATE_KEY, EtfMonitorConstants.UPDATES_CHANNEL),
                    json.writeValueAsString(enabled), UUID.randomUUID().toString().replace("-", ""));
            if (!Long.valueOf(1).equals(result)) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "ETF 启用清单发布失败");
            }
        } catch (JsonProcessingException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "ETF 启用清单编码失败");
        }
    }

    public void publishResync() {
        redis.execute(PUBLISH_SCRIPT, List.of(EtfMonitorConstants.ENABLED_KEY, EtfMonitorConstants.STATE_KEY, EtfMonitorConstants.UPDATES_CHANNEL),
                redis.opsForValue().get(EtfMonitorConstants.ENABLED_KEY), UUID.randomUUID().toString().replace("-", ""));
    }

    private List<EtfMonitorConfig> active() {
        return configMapper.selectList(new LambdaQueryWrapperX<EtfMonitorConfig>()
                .eq(EtfMonitorConfig::getEnabled, true)
                .orderByAsc(EtfMonitorConfig::getSortOrder)
                .orderByAsc(EtfMonitorConfig::getSymbol));
    }

    private void withLock(Runnable action) {
        String token = redisLock.acquire(EtfMonitorConstants.CONFIG_LOCK, Duration.ofSeconds(30));
        if (token == null) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "ETF 清单正在修改");
        }
        try {
            action.run();
        } finally {
            redisLock.release(EtfMonitorConstants.CONFIG_LOCK, token);
        }
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || !symbol.matches("^(SH|SZ)[0-9]{6}$")) {
            throw badRequest("ETF 标识格式错误");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ServiceException badRequest(String message) {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }
}
