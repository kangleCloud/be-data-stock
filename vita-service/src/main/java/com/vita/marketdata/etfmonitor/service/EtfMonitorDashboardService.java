package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.constant.MarketDataConstants;
import com.vita.marketdata.etfmonitor.constant.EtfMonitorConstants;
import com.vita.marketdata.etfmonitor.entity.EtfAssetAllocationReport;
import com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/** ETF 大屏只读取 Java 清单、Python Redis 快照和已经落库的真实资料。 */
@Service
public class EtfMonitorDashboardService {

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final EtfMonitorProfileMapper profiles;
    private final EtfAssetAllocationReportMapper allocations;
    private final boolean xqEnabled;

    public EtfMonitorDashboardService(StringRedisTemplate redis, ObjectMapper json,
                                      EtfMonitorProfileMapper profiles,
                                      EtfAssetAllocationReportMapper allocations,
                                      StockMonitorProperty property) {
        this.redis = redis;
        this.json = json;
        this.profiles = profiles;
        this.allocations = allocations;
        this.xqEnabled = property.isXqEnabled();
    }

    public ObjectNode dashboard() {
        for (int attempt = 0; attempt < 2; attempt++) {
            String stateId = validStateId(redis.opsForValue().get(EtfMonitorConstants.STATE_KEY));
            ObjectNode result = build(stateId);
            if (Objects.equals(stateId, redis.opsForValue().get(EtfMonitorConstants.STATE_KEY))) {
                return result;
            }
        }
        throw unavailable("ETF 数据正在更新，请重试");
    }

    private ObjectNode build(String stateId) {
        JsonNode enabled = parseRequired(redis.opsForValue().get(EtfMonitorConstants.ENABLED_KEY), "ETF 清单缓存不可用");
        if (!enabled.isArray() || enabled.size() > MarketDataConstants.MAX_MONITORS) {
            throw unavailable("ETF 清单缓存格式错误");
        }
        JsonNode snapshot = parseOptional(redis.opsForValue().get(EtfMonitorConstants.SNAPSHOT_KEY));
        if (snapshot != null && (snapshot.path("schemaVersion").asInt(-1) != 1
                || !"AKShare.fund_etf_category_sina".equals(snapshot.path("source").textValue())
                || !snapshot.path("items").isArray() || !validDate(snapshot.path("tradeDate").textValue()))) {
            throw unavailable("ETF 快照格式错误");
        }
        Map<String, JsonNode> bySymbol = new HashMap<>();
        if (snapshot != null) {
            for (JsonNode item : snapshot.path("items")) {
                String symbol = item.path("symbol").textValue();
                if (symbol != null && !symbol.isBlank() && bySymbol.putIfAbsent(symbol, item) != null) {
                    throw unavailable("ETF 快照含重复代码");
                }
            }
        }
        ObjectNode dashboard = json.createObjectNode();
        dashboard.put("schemaVersion", 1);
        putNullable(dashboard, "stateId", stateId);
        dashboard.put("xqEnabled", xqEnabled);
        putNullable(dashboard, "tradeDate", snapshot == null ? null : snapshot.path("tradeDate").textValue());
        ArrayNode etfs = dashboard.putArray("etfs");
        Set<String> seen = new HashSet<>();
        int order = 0;
        for (JsonNode entry : enabled) {
            String symbol = entry.path("symbol").textValue();
            String code = entry.path("code").textValue();
            String name = entry.path("name").textValue();
            String market = entry.path("market").textValue();
            if (symbol == null || !symbol.matches("^(SH|SZ)[0-9]{6}$")
                    || !symbol.substring(2).equals(code) || !symbol.substring(0, 2).equals(market)
                    || name == null || name.isBlank() || !seen.add(symbol)) {
                throw unavailable("ETF 清单字段不合法");
            }
            etfs.add(buildEtf(symbol, code, name, market, ++order, bySymbol.get(symbol)));
        }
        return dashboard;
    }

    private ObjectNode buildEtf(String symbol, String code, String name, String market,
                                int sortOrder, JsonNode item) {
        ObjectNode etf = json.createObjectNode();
        etf.put("symbol", symbol);
        etf.put("code", code);
        etf.put("name", name);
        etf.put("market", market);
        etf.put("sortOrder", sortOrder);
        etf.set("profile", profile(symbol));
        JsonNode rawQuote = item == null ? null : item.get("quote");
        String quoteDate = validQuoteDate(rawQuote);
        JsonNode quote = quoteDate == null ? null : rawQuote;
        String seriesDate = null;
        if (item != null && item.path("priceSeries").isArray()) {
            for (JsonNode point : item.path("priceSeries")) {
                String at = point.path("collectedAt").textValue();
                if (validTime(at) && point.path("price").isNumber()) {
                    String day = OffsetDateTime.parse(at).atZoneSameInstant(MarketDataConstants.SHANGHAI).toLocalDate().toString();
                    if (seriesDate == null || day.compareTo(seriesDate) > 0) seriesDate = day;
                }
            }
        }
        String effectiveDate = quoteDate == null ? seriesDate : seriesDate == null
                ? quoteDate : quoteDate.compareTo(seriesDate) >= 0 ? quoteDate : seriesDate;
        if (quote != null && !quoteDate.equals(effectiveDate)) quote = null;
        etf.set("quote", quote == null ? json.nullNode() : quote.deepCopy());
        ArrayNode series = json.createArrayNode();
        if (item != null && item.path("priceSeries").isArray()) {
            for (JsonNode point : item.path("priceSeries")) {
                String at = point.path("collectedAt").textValue();
                if (validTime(at) && effectiveDate != null && effectiveDate.equals(
                        OffsetDateTime.parse(at).atZoneSameInstant(MarketDataConstants.SHANGHAI).toLocalDate().toString())
                        && point.path("price").isNumber()) {
                    series.add(point);
                }
            }
        }
        etf.set("series", series);
        etf.set("fundSeries", json.createArrayNode());
        etf.put("fundFlowStatus", "NO_RELIABLE_SOURCE");
        putNullable(etf, "effectiveTradeDate", effectiveDate);
        String today = LocalDate.now(MarketDataConstants.SHANGHAI).toString();
        etf.put("dataStatus", effectiveDate == null ? "NO_DATA" : !today.equals(effectiveDate)
                ? "HISTORICAL" : quote != null && "FRESH".equals(quote.path("status").textValue())
                ? "CURRENT" : "DELAYED");
        etf.put("closeConfirmed", false);
        etf.set("assetAllocation", xqEnabled ? allocation(symbol) : json.nullNode());
        return etf;
    }

    private ObjectNode profile(String symbol) {
        EtfMonitorProfile profile = profiles.selectOne(EtfMonitorProfile::getSymbol, symbol);
        ObjectNode value = json.createObjectNode();
        if (profile == null || !"THS".equals(profile.getSource())) {
            for (String field : new String[]{"exchange", "etfType", "listingStatus", "listingDate",
                    "manager", "custodian", "shareCount", "shareDate", "trackingIndexCode",
                    "trackingIndexName", "updatedAt", "fullName", "fundType", "investmentType",
                    "fundManager", "establishedDate", "performanceBenchmark", "source"}) {
                value.putNull(field);
            }
            return value;
        }
        putNullable(value, "fullName", profile.getFullName());
        putNullable(value, "fundType", profile.getFundType());
        putNullable(value, "investmentType", profile.getInvestmentType());
        putNullable(value, "fundManager", profile.getFundManager());
        putNullable(value, "establishedDate", profile.getEstablishedDate() == null ? null
                : profile.getEstablishedDate().toString());
        putNullable(value, "performanceBenchmark", profile.getPerformanceBenchmark());
        // 只展示已成功取得的同花顺资料，旧交易所资料按空资料处理。
        putNullable(value, "source", profile.getSource());
        putNullable(value, "exchange", profile.getExchange());
        putNullable(value, "etfType", profile.getEtfType());
        putNullable(value, "listingStatus", profile.getListingStatus());
        putNullable(value, "listingDate", profile.getListingDate() == null ? null : profile.getListingDate().toString());
        putNullable(value, "manager", profile.getManager());
        putNullable(value, "custodian", profile.getCustodian());
        if (profile.getShareCount() == null) value.putNull("shareCount");
        else value.put("shareCount", profile.getShareCount());
        putNullable(value, "shareDate", profile.getShareDate() == null ? null : profile.getShareDate().toString());
        putNullable(value, "trackingIndexCode", profile.getTrackingIndexCode());
        putNullable(value, "trackingIndexName", profile.getTrackingIndexName());
        putNullable(value, "updatedAt", profile.getProfileUpdatedAt() == null ? null
                : profile.getProfileUpdatedAt().atZone(MarketDataConstants.SHANGHAI).toOffsetDateTime().toString());
        return value;
    }

    private JsonNode allocation(String symbol) {
        EtfAssetAllocationReport report = allocations.selectOne(new LambdaQueryWrapperX<EtfAssetAllocationReport>()
                .eq(EtfAssetAllocationReport::getSymbol, symbol)
                .orderByDesc(EtfAssetAllocationReport::getRequestedReportPeriod).last("LIMIT 1"));
        if (report == null) {
            return json.nullNode();
        }
        try {
            JsonNode categories = json.readTree(report.getCategoriesJson());
            if (!categories.isArray()) {
                return json.nullNode();
            }
            ObjectNode value = json.createObjectNode();
            value.put("requestedReportPeriod", report.getRequestedReportPeriod().toString());
            value.put("source", report.getSource());
            value.put("collectedAt", report.getCollectedAt().atZone(MarketDataConstants.SHANGHAI).toOffsetDateTime().toString());
            value.set("categories", categories);
            return value;
        } catch (JsonProcessingException exception) {
            throw unavailable("ETF 资产配置报告格式错误");
        }
    }

    private String validQuoteDate(JsonNode quote) {
        if (quote == null || !quote.isObject() || !"SINA_ETF".equals(quote.path("source").textValue())
                || !quote.path("sourceTime").isNull() || !quote.path("price").isNumber()
                || quote.path("price").decimalValue().signum() <= 0
                || !validTime(quote.path("collectedAt").textValue())) {
            return null;
        }
        String date = quote.path("tradeDate").textValue();
        return validDate(date) ? date : null;
    }

    private JsonNode parseRequired(String raw, String error) {
        if (raw == null) throw unavailable(error);
        return parseOptional(raw);
    }

    private JsonNode parseOptional(String raw) {
        if (raw == null) return null;
        try {
            return json.readTree(raw);
        } catch (JsonProcessingException exception) {
            throw unavailable("ETF 缓存 JSON 格式错误");
        }
    }

    private String validStateId(String value) {
        if (value != null && !value.matches("[0-9a-f]{32}")) {
            throw unavailable("ETF 版本格式错误");
        }
        return value;
    }

    private boolean validDate(String value) {
        try {
            return value != null && LocalDate.parse(value).toString().equals(value);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean validTime(String value) {
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void putNullable(ObjectNode node, String field, String value) {
        if (value == null) node.putNull(field);
        else node.put(field, value);
    }

    private ServiceException unavailable(String message) {
        return new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), message);
    }
}
