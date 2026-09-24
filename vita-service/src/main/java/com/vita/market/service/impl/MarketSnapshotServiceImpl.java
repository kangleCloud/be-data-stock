package com.vita.market.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.market.service.MarketSnapshotService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 从 AKShare 采集程序写入的普通 JSON 中读取市场快照。
 */
@Service
public class MarketSnapshotServiceImpl implements MarketSnapshotService {

    private static final String SNAPSHOT_KEY = "stock:market:v1:snapshot";
    private static final List<String> MODULE_NAMES = List.of(
            "industryHeatmap", "conceptHeatmap", "industryTop5", "conceptTop5", "marketFundFlow");
    private static final Set<String> STATUSES = Set.of("FRESH", "STALE", "ERROR");
    private static final Set<String> DATE_BASES = Set.of("CALENDAR", "SOURCE");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MarketSnapshotServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode getSnapshot() {
        // 采集端使用单键 SET；字符串模板保留原始 UTF-8 JSON，不应用 Java 类型序列化器。
        String raw = redisTemplate.opsForValue().get(SNAPSHOT_KEY);
        if (raw == null || raw.isBlank()) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "市场快照尚未生成");
        }

        try {
            JsonNode snapshot = objectMapper.readTree(raw);
            validate(snapshot);
            return snapshot;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "市场快照格式不正确");
        }
    }

    private void validate(JsonNode snapshot) {
        if (snapshot == null || !snapshot.isObject()
                || !snapshot.path("schemaVersion").isIntegralNumber()
                || snapshot.path("schemaVersion").intValue() != 1
                || !"akshare".equals(snapshot.path("provider").textValue())
                || !snapshot.path("generatedAt").isTextual()
                || !snapshot.path("modules").isObject()) {
            throw new IllegalArgumentException("市场快照顶层字段不合法");
        }

        JsonNode modules = snapshot.path("modules");
        for (String name : MODULE_NAMES) {
            JsonNode module = modules.path(name);
            if (!module.isObject() || !module.path("status").isTextual()
                    || !STATUSES.contains(module.path("status").textValue())
                    || !isNullableText(module, "tradeDate")
                    || !isNullableEnum(module, "tradeDateBasis", DATE_BASES)
                    || !isNullableText(module, "lastSuccessAt")
                    || !isNullableText(module, "lastAttemptAt")
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
                    || !module.path("lastSuccessAt").isTextual()) {
                throw new IllegalArgumentException("成功模块缺少数据或时间: " + name);
            }
            if (!data.isNull() && !validData(name, data)) {
                throw new IllegalArgumentException("市场快照模块数据不合法: " + name);
            }
        }
    }

    private boolean validData(String name, JsonNode data) {
        if (name.endsWith("Heatmap")) {
            return data.isArray();
        }
        if (name.endsWith("Top5")) {
            return data.isObject() && data.path("topRise").isArray()
                    && data.path("topFall").isArray() && data.path("topInflow").isArray()
                    && data.path("topOutflow").isArray()
                    && data.path("topRise").size() <= 5 && data.path("topFall").size() <= 5
                    && data.path("topInflow").size() <= 5 && data.path("topOutflow").size() <= 5
                    && data.path("unmatchedFundRows").isIntegralNumber();
        }
        return data.isObject() && data.path("latest").isObject()
                && data.path("series").isArray() && data.path("series").size() <= 20;
    }

    private boolean isNullableText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value != null && (value.isNull() || value.isTextual());
    }

    private boolean isNullableEnum(JsonNode object, String field, Set<String> allowed) {
        JsonNode value = object.get(field);
        return value != null && (value.isNull() || value.isTextual() && allowed.contains(value.textValue()));
    }
}
