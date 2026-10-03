package com.vita.etfmonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.etfmonitor.dto.EtfRefreshResult;
import com.vita.etfmonitor.entity.EtfAssetAllocationReport;
import com.vita.etfmonitor.entity.EtfMonitorProfile;
import com.vita.etfmonitor.entity.EtfSymbolDictionary;
import com.vita.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.etfmonitor.mapper.EtfSymbolDictionaryMapper;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.StockMonitorRedisLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/** 将 Python 已取得的真实 ETF 字典、交易所资料和资产类别占比写入 MySQL。 */
@Service
public class EtfMonitorRefreshService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final String CONFIG_LOCK = "stock:etf-monitor:v1:config:lock";
    private static final String REFRESH_LOCK = "stock:etf-monitor:v1:refresh:lock";

    private final EtfMonitorPythonClient python;
    private final EtfSymbolDictionaryMapper dictionaryMapper;
    private final EtfMonitorProfileMapper profileMapper;
    private final EtfAssetAllocationReportMapper allocationMapper;
    private final EtfMonitorConfigService configService;
    private final StockMonitorRedisLock lock;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final boolean xqEnabled;

    public EtfMonitorRefreshService(EtfMonitorPythonClient python,
                                    EtfSymbolDictionaryMapper dictionaryMapper,
                                    EtfMonitorProfileMapper profileMapper,
                                    EtfAssetAllocationReportMapper allocationMapper,
                                    EtfMonitorConfigService configService, StockMonitorRedisLock lock,
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
        return run(() -> {
            synchronizeDictionary();
            return synchronizeProfiles();
        });
    }

    public EtfRefreshResult refreshDictionary() {
        return run(() -> {
            synchronizeDictionary();
            return "SUCCESS";
        });
    }

    public EtfRefreshResult refreshProfiles() {
        return run(this::synchronizeProfiles);
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
        return run(() -> {
            synchronizeAllocation(symbol, reportPeriod);
            return "SUCCESS";
        });
    }

    private EtfRefreshResult run(Supplier<String> action) {
        String token = lock.acquire(REFRESH_LOCK, Duration.ofMinutes(15));
        if (token == null) {
            throw new ServiceException(GlobalErrorCode.LOCKED.getCode(), "ETF 同步正在执行");
        }
        String started = OffsetDateTime.now(SHANGHAI).toString();
        try {
            String status = action.get();
            return new EtfRefreshResult(status, started, OffsetDateTime.now(SHANGHAI).toString(),
                    "PARTIAL".equals(status) ? "部分交易所资料暂不可用" : null);
        } finally {
            lock.release(REFRESH_LOCK, token);
        }
    }

    private void synchronizeDictionary() {
        JsonNode response = python.dictionary();
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

    private String synchronizeProfiles() {
        List<String> symbols = configService.list().stream().map(item -> item.symbol()).toList();
        if (symbols.isEmpty()) return "SUCCESS";
        if (symbols.size() > 10) throw unavailable("ETF 启用数量超过 10");
        JsonNode response = python.profiles(Map.of("symbols", symbols, "asOfDate",
                LocalDate.now(SHANGHAI).format(DateTimeFormatter.BASIC_ISO_DATE)));
        if (response == null || response.path("schemaVersion").asInt(-1) != 1
                || !response.path("profiles").isArray()) {
            throw unavailable("ETF 资料响应格式错误");
        }
        LocalDateTime collectedAt = parseTime(response.path("collectedAt").textValue());
        Set<String> requested = Set.copyOf(symbols);
        Set<String> seen = new HashSet<>();
        List<EtfMonitorProfile> rows = new ArrayList<>();
        for (JsonNode item : response.path("profiles")) {
            String symbol = item.path("symbol").textValue();
            if (symbol == null || !requested.contains(symbol) || !seen.add(symbol)) {
                throw unavailable("ETF 资料代码不合法");
            }
            EtfMonitorProfile profile = new EtfMonitorProfile();
            profile.setSymbol(symbol);
            profile.setExchange(text(item, "exchange"));
            profile.setEtfType(text(item, "etfType"));
            profile.setListingDate(date(item, "listingDate"));
            profile.setManager(text(item, "manager"));
            profile.setCustodian(text(item, "custodian"));
            profile.setShareCount(number(item, "shareCount"));
            profile.setShareDate(date(item, "shareDate"));
            profile.setTrackingIndexCode(text(item, "trackingIndexCode"));
            profile.setTrackingIndexName(text(item, "trackingIndexName"));
            profile.setProfileUpdatedAt(collectedAt);
            rows.add(profile);
        }
        withConfigLock(() -> {
            transactions.executeWithoutResult(status -> {
                for (EtfMonitorProfile row : rows) {
                    EtfMonitorProfile old = profileMapper.selectOne(EtfMonitorProfile::getSymbol, row.getSymbol());
                    if (old == null) profileMapper.insert(row);
                    else {
                        row.setId(old.getId());
                        profileMapper.updateById(row);
                    }
                }
            });
            configService.publishResync();
        });
        JsonNode states = response.path("sourceStatus");
        return seen.size() == requested.size()
                && "OK".equals(states.path("sse").textValue())
                && "OK".equals(states.path("szse").textValue()) ? "SUCCESS" : "PARTIAL";
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
        String token = lock.acquire(CONFIG_LOCK, Duration.ofSeconds(60));
        if (token == null) throw unavailable("ETF 清单正在修改");
        try {
            action.run();
        } finally {
            lock.release(CONFIG_LOCK, token);
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
            return OffsetDateTime.parse(value).atZoneSameInstant(SHANGHAI).toLocalDateTime();
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
