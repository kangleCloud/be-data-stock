package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.constant.MarketDataConstants;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.etfmonitor.constant.EtfMonitorConstants;
import com.vita.marketdata.etfmonitor.dto.EtfRefreshResult;
import com.vita.marketdata.etfmonitor.entity.EtfAssetAllocationReport;
import com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile;
import com.vita.marketdata.etfmonitor.entity.EtfSymbolDictionary;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfSymbolDictionaryMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/** 将 Python 已取得的真实 ETF 字典、同花顺基本资料和资产类别占比写入 MySQL。 */
@Service
public class EtfMonitorRefreshService {

    private final EtfMonitorPythonClient python;
    private final EtfSymbolDictionaryMapper dictionaryMapper;
    private final EtfMonitorProfileMapper profileMapper;
    private final EtfAssetAllocationReportMapper allocationMapper;
    private final EtfMonitorConfigService configService;
    private final MarketDataRedisLock lock;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final boolean xqEnabled;

    public EtfMonitorRefreshService(EtfMonitorPythonClient python,
                                    EtfSymbolDictionaryMapper dictionaryMapper,
                                    EtfMonitorProfileMapper profileMapper,
                                    EtfAssetAllocationReportMapper allocationMapper,
                                    EtfMonitorConfigService configService, MarketDataRedisLock lock,
                                    TransactionTemplate transactions, ObjectMapper json,
                                    StockMonitorProperty property) {
        this.python = python;
        this.dictionaryMapper = dictionaryMapper;
        this.profileMapper = profileMapper;
        this.allocationMapper = allocationMapper;
        this.configService = configService;
        this.lock = lock;
        this.transactions = transactions;
        this.json = json;
        this.xqEnabled = property.isXqEnabled();
    }

    public EtfRefreshResult refresh() {
        return run(CollectionMode.AUTO, () -> {
            synchronizeDictionary(CollectionMode.AUTO);
            return synchronizeProfiles(CollectionMode.AUTO);
        });
    }

    public EtfRefreshResult refreshDictionary() {
        return refreshDictionary(CollectionMode.AUTO);
    }

    public EtfRefreshResult refreshDictionary(CollectionMode mode) {
        return run(mode, () -> {
            synchronizeDictionary(mode);
            return "SUCCESS";
        });
    }

    public EtfRefreshResult refreshProfiles() {
        return refreshProfiles(CollectionMode.AUTO);
    }

    public EtfRefreshResult refreshProfiles(CollectionMode mode) {
        return run(mode, () -> synchronizeProfiles(mode));
    }

    public EtfRefreshResult refreshAllocation(String symbol, String reportPeriod) {
        if (!xqEnabled) {
            throw unavailable("雪球采集总闸已关闭");
        }
        if (symbol == null || !symbol.matches("^(SH|SZ)[0-9]{6}$")
                || reportPeriod == null || !reportPeriod.matches("[0-9]{8}")) {
            throw badRequest("ETF 代码或报告期不合法");
        }
        try {
            LocalDate.parse(reportPeriod, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException exception) {
            throw badRequest("报告期日期不合法");
        }
        return run(CollectionMode.AUTO, () -> {
            synchronizeAllocation(symbol, reportPeriod);
            return "SUCCESS";
        });
    }

    private EtfRefreshResult run(CollectionMode mode, Supplier<String> action) {
        // MANUAL 仅绕过采集准入；后续 CONFIG_LOCK、事务和 resync 与 AUTO 共用。
        String token = mode == CollectionMode.MANUAL ? null
                : lock.acquire(EtfMonitorConstants.REFRESH_LOCK, Duration.ofMinutes(15));
        if (mode != CollectionMode.MANUAL && token == null) {
            throw new ServiceException(GlobalErrorCode.LOCKED.getCode(), "ETF 同步正在执行");
        }
        String started = OffsetDateTime.now(MarketDataConstants.SHANGHAI).toString();
        try {
            String status = action.get();
            return new EtfRefreshResult(status, started, OffsetDateTime.now(MarketDataConstants.SHANGHAI).toString(),
                    "PARTIAL".equals(status) ? "部分同花顺基本资料暂不可用，失败项保留已成功取得的同花顺资料" : null);
        } finally {
            if (token != null) lock.release(EtfMonitorConstants.REFRESH_LOCK, token);
        }
    }

    private void synchronizeDictionary(CollectionMode mode) {
        JsonNode response = python.dictionary(mode);
        if (response == null || response.path("schemaVersion").asInt(-1) != 1
                || !"SINA".equals(response.path("source").textValue())
                || !response.path("etfs").isArray() || response.path("etfs").isEmpty()) {
            throw unavailable("ETF 字典响应格式错误");
        }
        LocalDateTime collectedAt = parseTime(response.path("collectedAt").textValue());
        List<EtfSymbolDictionary> rows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode item : response.path("etfs")) {
            String symbol = item.path("symbol").textValue();
            String code = item.path("code").textValue();
            String market = item.path("market").textValue();
            String name = item.path("name").textValue();
            if (symbol == null || !symbol.matches("^(SH|SZ)[0-9]{6}$")
                    || !symbol.substring(2).equals(code) || !symbol.substring(0, 2).equals(market)
                    || name == null || name.isBlank() || !seen.add(symbol)) {
                throw unavailable("ETF 字典行不合法");
            }
            EtfSymbolDictionary entity = new EtfSymbolDictionary();
            entity.setSymbol(symbol);
            entity.setCode(code);
            entity.setName(name);
            entity.setMarket(market);
            entity.setExchange(text(item, "exchange"));
            entity.setEtfType(text(item, "etfType"));
            entity.setListingStatus(text(item, "listingStatus"));
            entity.setListingDate(date(item, "listingDate"));
            entity.setTrackingIndexCode(text(item, "trackingIndexCode"));
            entity.setTrackingIndexName(text(item, "trackingIndexName"));
            entity.setSource("SINA");
            entity.setSyncedAt(collectedAt);
            rows.add(entity);
        }
        withConfigLock(() -> {
            transactions.executeWithoutResult(status -> {
                for (int start = 0; start < rows.size(); start += 300) {
                    dictionaryMapper.upsertBatch(rows.subList(start, Math.min(start + 300, rows.size())));
                }
            });
            configService.rebuildEnabledCache();
        });
    }

    private String synchronizeProfiles(CollectionMode mode) {
        List<String> symbols = configService.list().stream().map(item -> item.symbol()).toList();
        if (symbols.isEmpty()) return "SUCCESS";
        if (symbols.size() > MarketDataConstants.MAX_MONITORS) throw unavailable("ETF 启用数量超过 10");
        // 基本资料不受雪球总闸控制，也不进入 120 秒行情采样；Python 独立负责 30 分钟间隔。
        JsonNode response = python.profiles(Map.of("symbols", symbols), mode);
        if (response == null || response.path("schemaVersion").asInt(-1) != 1
                || !"THS".equals(response.path("source").textValue())
                || !response.path("profiles").isArray() || !response.path("sourceStatus").isObject()) {
            throw unavailable("同花顺 ETF 资料响应格式错误");
        }
        parseTime(response.path("collectedAt").textValue());
        Set<String> requested = Set.copyOf(symbols);
        JsonNode states = response.path("sourceStatus");
        Set<String> statusSymbols = new HashSet<>();
        states.fieldNames().forEachRemaining(statusSymbols::add);
        if (!statusSymbols.equals(requested)) throw unavailable("ETF 资料状态代码不匹配");
        for (String symbol : symbols) {
            if (!Set.of("OK", "ERROR", "SKIPPED").contains(states.path(symbol).asText())) {
                throw unavailable("ETF 资料状态不合法");
            }
        }
        Set<String> seen = new HashSet<>();
        List<EtfMonitorProfile> rows = new ArrayList<>();
        for (JsonNode item : response.path("profiles")) {
            String symbol = item.path("symbol").textValue();
            if (symbol == null || !symbol.matches("^(SH|SZ)[0-9]{6}$")
                    || !requested.contains(symbol) || !seen.add(symbol)
                    || !"OK".equals(states.path(symbol).textValue())) {
                throw unavailable("ETF 资料代码或状态不一致");
            }
            try {
                rows.add(thsProfile(item, symbol));
            } catch (ServiceException exception) {
                // 单股格式错误不覆盖已成功取得的同花顺资料，也不阻止其他已成功的资料落库。
            }
        }
        if (rows.isEmpty()) throw unavailable("本次未取得有效同花顺 ETF 资料，未覆盖已成功取得的同花顺资料");
        withConfigLock(() -> {
            transactions.executeWithoutResult(status -> {
                for (EtfMonitorProfile row : rows) {
                    // 字典分类和已核实指数只读字典；基准文本绝不推断跟踪指数代码。
                    EtfSymbolDictionary dictionary = dictionaryMapper.selectOne(
                            EtfSymbolDictionary::getSymbol, row.getSymbol());
                    row.setExchange(dictionary == null ? null : dictionary.getExchange());
                    row.setEtfType(dictionary == null ? null : dictionary.getEtfType());
                    row.setTrackingIndexCode(dictionary == null ? null : dictionary.getTrackingIndexCode());
                    row.setTrackingIndexName(dictionary == null ? null : dictionary.getTrackingIndexName());
                    EtfMonitorProfile old = profileMapper.selectOne(EtfMonitorProfile::getSymbol, row.getSymbol());
                    if (old == null) profileMapper.insert(row);
                    else {
                        row.setId(old.getId());
                        profileMapper.updateThsProfile(row);
                    }
                }
            });
            configService.publishResync();
        });
        return rows.size() == requested.size() ? "SUCCESS" : "PARTIAL";
    }

    private EtfMonitorProfile thsProfile(JsonNode item, String symbol) {
        if (!"THS".equals(item.path("source").textValue())
                || (item.has("code") && !symbol.substring(2).equals(item.path("code").textValue()))) {
            throw unavailable("同花顺 ETF 资料来源或代码不匹配");
        }
        EtfMonitorProfile profile = new EtfMonitorProfile();
        profile.setSymbol(symbol);
        profile.setFullName(profileText(item, "fullName", 200));
        profile.setFundType(profileText(item, "fundType", 100));
        profile.setInvestmentType(profileText(item, "investmentType", 100));
        profile.setFundManager(profileText(item, "fundManager", 200));
        String established = profileText(item, "establishedDate", 10);
        try {
            profile.setEstablishedDate(established == null ? null : LocalDate.parse(established));
        } catch (RuntimeException exception) {
            throw unavailable("同花顺成立日期格式错误");
        }
        profile.setPerformanceBenchmark(profileText(item, "performanceBenchmark", 1000));
        profile.setManager(profileText(item, "manager", 100));
        profile.setCustodian(profileText(item, "custodian", 100));
        if (profile.getFullName() == null && profile.getFundType() == null
                && profile.getInvestmentType() == null && profile.getFundManager() == null
                && profile.getEstablishedDate() == null && profile.getPerformanceBenchmark() == null
                && profile.getManager() == null && profile.getCustodian() == null) {
            throw unavailable("同花顺 ETF 资料为空");
        }
        profile.setSource("THS");
        // 每只基金以实际采集时间记录，不用批次时间或行情源时间替代。
        profile.setProfileUpdatedAt(parseTime(item.path("collectedAt").textValue()));
        return profile;
    }

    private String profileText(JsonNode item, String field, int maxLength) {
        JsonNode value = item.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual() || value.textValue().length() > maxLength) {
            throw unavailable("同花顺 ETF 资料字段格式错误");
        }
        return value.textValue().isBlank() ? null : value.textValue();
    }

    private void synchronizeAllocation(String symbol, String reportPeriod) {
        if (configService.list().stream().noneMatch(item -> symbol.equals(item.symbol()))) {
            throw badRequest("只能同步已启用 ETF 的资产配置");
        }
        JsonNode response = python.assetAllocation(Map.of("symbol", symbol, "reportPeriod", reportPeriod));
        if (response == null || response.path("schemaVersion").asInt(-1) != 1
                || !symbol.equals(response.path("symbol").textValue())
                || !"XQ_DANJUAN".equals(response.path("source").textValue())
                || !response.path("categories").isArray() || response.path("categories").isEmpty()) {
            throw unavailable("ETF 资产配置响应格式错误");
        }
        LocalDate period = date(response, "requestedReportPeriod");
        if (period == null || !period.format(DateTimeFormatter.BASIC_ISO_DATE).equals(reportPeriod)) {
            throw unavailable("ETF 报告期不匹配");
        }
        for (JsonNode category : response.path("categories")) {
            BigDecimal percent = number(category, "percent");
            if (text(category, "category") == null || percent == null
                    || percent.signum() < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw unavailable("ETF 资产类别占比不合法");
            }
        }
        EtfAssetAllocationReport report = new EtfAssetAllocationReport();
        report.setSymbol(symbol);
        report.setRequestedReportPeriod(period);
        report.setSource("XQ_DANJUAN");
        report.setCollectedAt(parseTime(response.path("collectedAt").textValue()));
        try {
            report.setCategoriesJson(json.writeValueAsString(response.path("categories")));
        } catch (JsonProcessingException exception) {
            throw unavailable("ETF 资产配置编码失败");
        }
        withConfigLock(() -> {
            transactions.executeWithoutResult(status -> {
                EtfAssetAllocationReport old = allocationMapper.selectOne(
                        new com.vita.mybatis.wrapper.LambdaQueryWrapperX<EtfAssetAllocationReport>()
                                .eq(EtfAssetAllocationReport::getSymbol, symbol)
                                .eq(EtfAssetAllocationReport::getRequestedReportPeriod, period));
                if (old == null) allocationMapper.insert(report);
                else {
                    report.setId(old.getId());
                    allocationMapper.updateById(report);
                }
            });
            configService.publishResync();
        });
    }

    private void withConfigLock(Runnable action) {
        String token = lock.acquire(EtfMonitorConstants.CONFIG_LOCK, Duration.ofSeconds(60));
        if (token == null) throw unavailable("ETF 清单正在修改");
        try {
            action.run();
        } finally {
            lock.release(EtfMonitorConstants.CONFIG_LOCK, token);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.textValue().isBlank() ? value.textValue() : null;
    }

    private LocalDate date(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw unavailable("ETF 日期格式错误");
        }
    }

    private BigDecimal number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.isNumber() ? value.decimalValue() : null;
    }

    private LocalDateTime parseTime(String value) {
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(MarketDataConstants.SHANGHAI).toLocalDateTime();
        } catch (RuntimeException exception) {
            throw unavailable("ETF 采集时间格式错误");
        }
    }

    private ServiceException unavailable(String message) {
        return new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), message);
    }

    private ServiceException badRequest(String message) {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }
}
