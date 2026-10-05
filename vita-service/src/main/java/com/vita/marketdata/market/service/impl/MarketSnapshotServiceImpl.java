package com.vita.marketdata.market.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.constant.MarketDataConstants;
import com.vita.marketdata.market.constant.MarketConstants;
import com.vita.marketdata.market.dto.MarketIndexConfigDto;
import com.vita.marketdata.market.enums.CoreIndexEnum;
import com.vita.marketdata.market.service.MarketIndexConfigService;
import com.vita.marketdata.market.service.MarketSnapshotService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 从 AKShare 采集程序写入的普通 JSON 中读取市场快照。
 */
@Service
public class MarketSnapshotServiceImpl implements MarketSnapshotService {

    private static final List<String> MODULE_NAMES = List.of(
            "industrySectors", "conceptSectors", "marketFundFlow");
    private static final Set<String> INDEX_DATA_FIELDS = Set.of("source", "sourceTime", "items");
    private static final Set<String> INDEX_ITEM_FIELDS = Set.of("code", "name", "price", "change",
            "changePercent", "previousClose", "open", "high", "low", "volume", "amount",
            "sourceTime", "collectedAt", "series");
    private static final Set<String> INDEX_POINT_FIELDS = Set.of("collectedAt", "price");
    private static final Set<String> STATUSES = Set.of("FRESH", "STALE", "ERROR");
    private static final Set<String> SECTOR_DATA_FIELDS = Set.of("source", "period", "items");
    private static final Set<String> SECTOR_ITEM_FIELDS = Set.of("code", "name", "type", "indexValue",
            "changePct", "inflow", "outflow", "netAmount", "netFlowRate", "companyCount",
            "leader", "leaderChangePct", "leaderPrice");
    private static final Set<String> MARKET_DATA_FIELDS = Set.of("source", "latest", "series");
    private static final Set<String> MARKET_LATEST_FIELDS = Set.of("collectedAt", "inflow", "outflow",
            "netAmount", "riseCount", "fallCount", "flatCount", "stockCount");
    private static final Set<String> MARKET_POINT_FIELDS = Set.of("collectedAt", "inflow", "outflow", "netAmount");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MarketIndexConfigService indexConfigService;

    public MarketSnapshotServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                     MarketIndexConfigService indexConfigService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.indexConfigService = indexConfigService;
    }

    @Override
    public JsonNode getSnapshot() {
        // 采集端使用单键 SET；字符串模板保留原始 UTF-8 JSON，不应用 Java 类型序列化器。
        String raw = redisTemplate.opsForValue().get(MarketConstants.SNAPSHOT_KEY);
        if (raw == null || raw.isBlank()) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "市场快照尚未生成");
        }

        try {
            JsonNode snapshot = objectMapper.readTree(raw);
            validate(snapshot);
            reconcile(snapshot);
            if (snapshot.path("modules").has("coreIndices")) {
                List<MarketIndexConfigDto> configs = indexConfigService.enabled();
                JsonNode indexData = snapshot.path("modules").path("coreIndices").path("data");
                if (indexData.isObject()) {
                    ObjectNode data = (ObjectNode) indexData;
                    com.fasterxml.jackson.databind.node.ArrayNode visible = objectMapper.createArrayNode();
                    for (MarketIndexConfigDto config : configs) {
                        for (JsonNode item : data.path("items")) {
                            if (config.code().equals(item.path("code").textValue())) {
                                visible.add(item);
                                break;
                            }
                        }
                    }
                    data.set("items", visible);
                }
                String rawId = snapshot.path("snapshotId").textValue();
                if (rawId != null) {
                    ((ObjectNode) snapshot).put("snapshotId", publicSnapshotId(rawId, configs));
                }
            }
            return snapshot;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "市场快照格式不正确");
        }
    }

    @Override
    public String publicSnapshotId(String rawSnapshotId) {
        if (rawSnapshotId == null) {
            return null;
        }
        return publicSnapshotId(rawSnapshotId, indexConfigService.enabled());
    }

    private String publicSnapshotId(String rawSnapshotId, List<MarketIndexConfigDto> configs) {
        String suffix = configs.stream().map(MarketIndexConfigDto::code)
                .reduce("", (left, right) -> left + "," + right);
        return UUID.nameUUIDFromBytes((rawSnapshotId + suffix).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }

    private void validate(JsonNode snapshot) {
        if (snapshot == null || !snapshot.isObject()
                || !snapshot.path("schemaVersion").isIntegralNumber()
                || !snapshot.path("schemaVersion").canConvertToInt()
                || snapshot.path("schemaVersion").intValue() != 1
                || !"akshare".equals(snapshot.path("provider").textValue())
                || !validOffsetTime(snapshot.path("generatedAt"))
                || !validSnapshotId(snapshot.get("snapshotId"))
                || !snapshot.path("modules").isObject()) {
            throw new IllegalArgumentException("市场快照顶层字段不合法");
        }

        JsonNode modules = snapshot.path("modules");
        Set<String> names = new java.util.HashSet<>();
        modules.fieldNames().forEachRemaining(names::add);
        if (!names.equals(Set.copyOf(MODULE_NAMES))
                && !names.equals(Set.of("industrySectors", "conceptSectors", "marketFundFlow", "coreIndices"))) {
            // 旧榜单与东财字段不能混入 V1 新快照，避免页面误用不同来源口径。
            throw new IllegalArgumentException("市场快照模块集合不合法");
        }
        for (String name : names) {
            JsonNode module = modules.path(name);
            if (!module.isObject() || !module.path("status").isTextual()
                    || !STATUSES.contains(module.path("status").textValue())
                    || !isNullableDate(module.get("tradeDate"))
                    || !"CALENDAR".equals(module.path("tradeDateBasis").textValue())
                    || !isNullableOffsetTime(module.get("lastSuccessAt"))
                    || !validOffsetTime(module.get("lastAttemptAt"))
                    || !isNullableText(module, "message")
                    || !module.has("data")) {
                throw new IllegalArgumentException("市场快照模块字段不合法: " + name);
            }
            JsonNode data = module.path("data");
            if ("ERROR".equals(module.path("status").textValue())) {
                if (!data.isNull()) {
                    throw new IllegalArgumentException("首次失败模块数据必须为空: " + name);
                }
            } else if (data.isNull() || !module.path("tradeDate").isTextual()
                    || !validOffsetTime(module.get("lastSuccessAt"))) {
                throw new IllegalArgumentException("成功模块缺少数据或时间: " + name);
            }
            if (!data.isNull() && !validData(name, data, module.path("tradeDate").textValue())) {
                throw new IllegalArgumentException("市场快照模块数据不合法: " + name);
            }
        }
    }

    private boolean validData(String name, JsonNode data, String tradeDate) {
        if ("coreIndices".equals(name)) {
            return validIndexData(data, tradeDate);
        }
        if (name.endsWith("Sectors")) {
            return validSectorData(name, data);
        }
        return validMarketData(data, tradeDate);
    }

    private boolean validIndexData(JsonNode data, String tradeDate) {
        if (!data.isObject() || !hasExactlyFields(data, INDEX_DATA_FIELDS)
                || !"SINA_INDEX".equals(data.path("source").textValue())
                || !data.path("sourceTime").isNull() || !data.path("items").isArray()
                || data.path("items").size() != CoreIndexEnum.codes().size()) {
            return false;
        }
        Set<String> seen = new java.util.HashSet<>();
        for (JsonNode item : data.path("items")) {
            if (!item.isObject() || !hasExactlyFields(item, INDEX_ITEM_FIELDS)
                    || !CoreIndexEnum.codes().contains(item.path("code").textValue())
                    || !seen.add(item.path("code").textValue())
                    || !item.path("name").isTextual() || item.path("name").textValue().isBlank()
                    || !isFiniteNumber(item, "price") || item.path("price").decimalValue().signum() <= 0
                    || !isNullableFiniteNumber(item, "change")
                    || !isNullableFiniteNumber(item, "changePercent")
                    || !isNullableFiniteNumber(item, "previousClose")
                    || !isNullableFiniteNumber(item, "open") || !isNullableFiniteNumber(item, "high")
                    || !isNullableFiniteNumber(item, "low") || !isNullableFiniteNumber(item, "volume")
                    || !isNullableFiniteNumber(item, "amount") || !item.path("sourceTime").isNull()
                    || !validCollectionTime(item.get("collectedAt"), tradeDate)
                    || !item.path("series").isArray()) {
                return false;
            }
            Instant previous = null;
            for (JsonNode point : item.path("series")) {
                if (!point.isObject() || !hasExactlyFields(point, INDEX_POINT_FIELDS)
                        || !validCollectionTime(point.get("collectedAt"), tradeDate)
                        || !isFiniteNumber(point, "price") || point.path("price").decimalValue().signum() <= 0) {
                    return false;
                }
                Instant at = OffsetDateTime.parse(point.path("collectedAt").textValue()).toInstant();
                if (previous != null && !at.isAfter(previous)) {
                    return false;
                }
                previous = at;
            }
        }
        return seen.equals(CoreIndexEnum.codes());
    }

    private boolean validSectorData(String moduleName, JsonNode data) {
        if (!data.isObject() || !hasExactlyFields(data, SECTOR_DATA_FIELDS)
                || !"THS".equals(data.path("source").textValue())
                || !"INTRADAY".equals(data.path("period").textValue())
                || !data.path("items").isArray()) {
            return false;
        }
        String sectorType = moduleName.startsWith("industry") ? "industry" : "concept";
        for (JsonNode item : data.path("items")) {
            if (!item.isObject() || !hasExactlyFields(item, SECTOR_ITEM_FIELDS)
                    || !isNullableNonBlankText(item, "code")
                    || !item.path("name").isTextual() || item.path("name").textValue().isBlank()
                    || !sectorType.equals(item.path("type").textValue())
                    || !isNullableNonBlankText(item, "leader")
                    || !isNullableFiniteNumber(item, "indexValue")
                    || !isNullableFiniteNumber(item, "changePct")
                    || !isNullableFiniteNumber(item, "inflow")
                    || !isNullableFiniteNumber(item, "outflow")
                    || !isNullableFiniteNumber(item, "netAmount")
                    || !isNullableFiniteNumber(item, "netFlowRate")
                    || !isNullableCount(item, "companyCount")
                    || !isNullableFiniteNumber(item, "leaderChangePct")
                    || !isNullableFiniteNumber(item, "leaderPrice")) {
                return false;
            }
        }
        return true;
    }

    private boolean validMarketData(JsonNode data, String tradeDate) {
        if (!data.isObject() || !hasMarketFields(data)
                || !"THS_INDIVIDUAL_AGGREGATE".equals(data.path("source").textValue())
                || data.has("reconciledFromLegacy") && !data.path("reconciledFromLegacy").isBoolean()
                || !data.path("latest").isObject() || !data.path("series").isArray()) {
            return false;
        }
        JsonNode latest = data.path("latest");
        if (!hasExactlyFields(latest, MARKET_LATEST_FIELDS)
                || !validCollectionTime(latest.get("collectedAt"), tradeDate)
                || !validMarketAmounts(latest)
                || !isCount(latest, "riseCount") || !isCount(latest, "fallCount")
                || !isCount(latest, "flatCount") || !isCount(latest, "stockCount")
                || !validCountTotal(latest)) {
            return false;
        }
        Instant previous = null;
        for (JsonNode point : data.path("series")) {
            // 曲线只接受上海交易日内真实且递增的采集时刻，不替采集端补点。
            if (!point.isObject() || !hasExactlyFields(point, MARKET_POINT_FIELDS)
                    || !validCollectionTime(point.get("collectedAt"), tradeDate)
                    || !validMarketAmounts(point)) {
                return false;
            }
            Instant collectedAt = OffsetDateTime.parse(point.path("collectedAt").textValue()).toInstant();
            if (previous != null && !collectedAt.isAfter(previous)) {
                return false;
            }
            previous = collectedAt;
        }
        return true;
    }

    private boolean validMarketAmounts(JsonNode object) {
        return isFiniteNumber(object, "inflow")
                && isFiniteNumber(object, "outflow")
                && isNullableFiniteNumber(object, "netAmount");
    }

    private boolean hasMarketFields(JsonNode data) {
        Set<String> fields = new java.util.HashSet<>();
        data.fieldNames().forEachRemaining(fields::add);
        fields.remove("reconciledFromLegacy");
        return fields.equals(MARKET_DATA_FIELDS);
    }

    private boolean validCountTotal(JsonNode latest) {
        BigInteger total = latest.path("riseCount").bigIntegerValue()
                .add(latest.path("fallCount").bigIntegerValue())
                .add(latest.path("flatCount").bigIntegerValue());
        return total.equals(latest.path("stockCount").bigIntegerValue());
    }

    private void reconcile(JsonNode snapshot) {
        ObjectNode root = (ObjectNode) snapshot;
        if (!root.has("snapshotId")) {
            root.putNull("snapshotId");
        }
        JsonNode data = snapshot.path("modules").path("marketFundFlow").path("data");
        if (data.isNull()) {
            return;
        }
        boolean corrected = reconcileAmount((ObjectNode) data.path("latest"));
        for (JsonNode point : data.path("series")) {
            corrected |= reconcileAmount((ObjectNode) point);
        }
        ((ObjectNode) data).put("reconciledFromLegacy",
                corrected || data.path("reconciledFromLegacy").asBoolean(false));
    }

    private boolean reconcileAmount(ObjectNode values) {
        BigDecimal net = values.path("inflow").decimalValue().subtract(values.path("outflow").decimalValue());
        JsonNode old = values.get("netAmount");
        if (old != null && old.isNumber() && old.decimalValue().compareTo(net) == 0) {
            return false;
        }
        values.put("netAmount", net);
        return true;
    }

    private boolean validSnapshotId(JsonNode value) {
        if (value == null || value.isNull()) {
            return true;
        }
        try {
            return value.isTextual() && (value.textValue().matches("[0-9a-f]{32}")
                    || UUID.fromString(value.textValue()).toString().equals(value.textValue()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isFiniteNumber(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && value.isNumber()
                && (!value.isFloatingPointNumber() || Double.isFinite(value.doubleValue()));
    }

    private boolean isCount(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0;
    }

    private boolean validCollectionTime(JsonNode time, String tradeDate) {
        return validOffsetTime(time) && tradeDate.equals(OffsetDateTime.parse(time.textValue())
                .atZoneSameInstant(MarketDataConstants.SHANGHAI).toLocalDate().toString());
    }

    private boolean hasExactlyFields(JsonNode object, Set<String> expectedFields) {
        Set<String> actualFields = new java.util.HashSet<>();
        object.fieldNames().forEachRemaining(actualFields::add);
        return actualFields.equals(expectedFields);
    }

    private boolean isNullableText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && (value.isNull() || value.isTextual());
    }

    private boolean isNullableNonBlankText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && (value.isNull() || value.isTextual() && !value.textValue().isBlank());
    }

    private boolean isNullableDate(JsonNode value) {
        if (value == null || value.isNull()) {
            return value != null;
        }
        try {
            return value.isTextual() && LocalDate.parse(value.textValue()).toString().equals(value.textValue());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isNullableOffsetTime(JsonNode value) {
        return value != null && (value.isNull() || validOffsetTime(value));
    }

    private boolean validOffsetTime(JsonNode value) {
        try {
            if (value == null || !value.isTextual()) {
                return false;
            }
            OffsetDateTime.parse(value.textValue());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isNullableFiniteNumber(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && (value.isNull()
                || value.isNumber() && (!value.isFloatingPointNumber() || Double.isFinite(value.doubleValue())));
    }

    private boolean isNullableCount(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && (value.isNull() || value.isIntegralNumber() && value.canConvertToLong()
                && value.longValue() >= 0);
    }
}
