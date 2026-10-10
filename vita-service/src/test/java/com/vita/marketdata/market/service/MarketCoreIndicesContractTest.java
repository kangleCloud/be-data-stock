package com.vita.marketdata.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.market.dto.MarketIndexConfigDto;
import com.vita.marketdata.market.service.impl.MarketSnapshotServiceImpl;
import com.vita.marketdata.support.SseEventCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketCoreIndicesContractTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void acceptsFiveSinaRowsButPublishesOnlyConfiguredOrderAndVersion() {
        ObjectNode raw = rawSnapshot(true);
        MarketIndexConfigService config = mock(MarketIndexConfigService.class);
        when(config.enabled()).thenReturn(List.of(
                new MarketIndexConfigDto("sz399006", "创业板指", true, 1),
                new MarketIndexConfigDto("sh000001", "上证指数", true, 2)));
        var snapshots = service(raw.toString(), config);
        var result = snapshots.getSnapshot();
        var visible = result.path("modules").path("coreIndices").path("data").path("items");
        assertEquals(2, visible.size());
        assertEquals("sz399006", visible.get(0).path("code").asText());
        assertEquals("sh000001", visible.get(1).path("code").asText());
        assertNotEquals(raw.path("snapshotId").asText(), result.path("snapshotId").asText());
        assertEquals(snapshots.publicSnapshotId(raw.path("snapshotId").asText()),
                result.path("snapshotId").asText(), "SSE 通知映射与匿名 GET 必须使用相同公开版本");
    }

    @Test
    void retainsOldThreeModuleCacheDuringProducerRollout() {
        ObjectNode raw = rawSnapshot(false);
        MarketIndexConfigService config = mock(MarketIndexConfigService.class);
        var result = service(raw.toString(), config).getSnapshot();
        assertEquals(raw.path("snapshotId").asText(), result.path("snapshotId").asText());
    }

    @Test
    void rejectsUnknownCoreIndexCode() {
        ObjectNode raw = rawSnapshot(true);
        ((ObjectNode) raw.path("modules").path("coreIndices").path("data")
                .path("items").get(0)).put("code", "sh000002");
        assertThrows(ServiceException.class, () -> service(raw.toString(), mock(MarketIndexConfigService.class))
                .getSnapshot());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FRESH", "STALE"})
    void acceptsUndatedSourceValuesWithEmptyCurves(String status) {
        var raw = undatedSnapshot(status);
        var result = service(raw.toString(), config()).getSnapshot();
        var modules = result.path("modules");
        assertTrue(modules.path("marketFundFlow").path("tradeDate").isNull());
        assertTrue(modules.path("marketFundFlow").path("data").path("series").isEmpty());
        assertEquals(80, modules.path("marketFundFlow").path("data").path("latest").path("netAmount").asInt());
        var row = modules.path("coreIndices").path("data").path("items").get(0);
        assertEquals("2026-10-01T21:00:00+08:00", row.path("collectedAt").asText());
        assertTrue(row.path("series").isEmpty());
        assertTrue(row.path("sourceTime").isNull());
    }

    @ParameterizedTest
    @ValueSource(strings = {"indexPoint", "fundPoint", "badCollectedAt", "badSuccess", "badAttempt",
            "badSource", "badPrice", "badInflow", "badNet", "badCounts", "negativeCount"})
    void undatedSnapshotStillRejectsInvalidData(String scenario) {
        var raw = undatedSnapshot("FRESH");
        var modules = raw.path("modules");
        var fund = (ObjectNode) modules.path("marketFundFlow");
        var data = (ObjectNode) fund.path("data");
        var latest = (ObjectNode) data.path("latest");
        var row = (ObjectNode) modules.path("coreIndices").path("data").path("items").get(0);
        switch (scenario) {
            case "indexPoint" -> ((ArrayNode) row.path("series")).addObject()
                    .put("collectedAt", "2026-10-01T21:00:00+08:00").put("price", 3000);
            case "fundPoint" -> ((ArrayNode) data.path("series")).add(latest.deepCopy());
            case "badCollectedAt" -> latest.put("collectedAt", "2026-10-01T21:00:00");
            case "badSuccess" -> fund.putNull("lastSuccessAt");
            case "badAttempt" -> fund.put("lastAttemptAt", "invalid");
            case "badSource" -> data.put("source", "OTHER");
            case "badPrice" -> row.put("price", 0);
            case "badInflow" -> latest.put("inflow", "100");
            case "badNet" -> latest.put("netAmount", "80");
            case "badCounts" -> latest.put("stockCount", 9);
            case "negativeCount" -> latest.put("riseCount", -1);
            default -> fail("未知测试场景");
        }
        assertThrows(ServiceException.class, () -> service(raw.toString(), config()).getSnapshot());
    }

    @ParameterizedTest
    @ValueSource(strings = {"indexOtherDay", "indexDuplicate", "indexReverse", "fundOtherDay", "fundDuplicate", "fundReverse"})
    void knownDateStillRequiresSameDayAndStrictlyIncreasingPoints(String scenario) {
        var raw = undatedSnapshot("FRESH");
        var modules = raw.path("modules");
        ((ObjectNode) modules.path("coreIndices")).put("tradeDate", "2026-10-01");
        ((ObjectNode) modules.path("marketFundFlow")).put("tradeDate", "2026-10-01");
        ArrayNode points = scenario.startsWith("index")
                ? (ArrayNode) modules.path("coreIndices").path("data").path("items").get(0).path("series")
                : (ArrayNode) modules.path("marketFundFlow").path("data").path("series");
        var first = points.addObject().put("collectedAt", "2026-10-01T10:00:00+08:00");
        if (scenario.startsWith("index")) first.put("price", 3000);
        else first.put("inflow", 100).put("outflow", 20).put("netAmount", 80);
        var second = first.deepCopy();
        second.put("collectedAt", scenario.endsWith("OtherDay") ? "2026-10-02T10:01:00+08:00"
                : scenario.endsWith("Reverse") ? "2026-10-01T09:59:00+08:00" : "2026-10-01T10:00:00+08:00");
        points.add(second);
        assertThrows(ServiceException.class, () -> service(raw.toString(), config()).getSnapshot());
    }

    @Test
    void realReaderPublishesUndatedSsePatchAndResyncsOnPoints() throws Exception {
        var raw = undatedSnapshot("FRESH");
        raw.put("snapshotId", SseEventCapture.id(1));
        var cache = new AtomicReference<>(raw.toString());
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:market:v1:snapshot")).thenAnswer(call -> cache.get());
        var reader = new MarketSnapshotServiceImpl(redis, json, config());
        var stream = spy(new MarketSnapshotStreamService(reader, json, mock(ScheduledExecutorService.class)));
        var emitter = mock(SseEmitter.class);
        var events = SseEventCapture.attach(emitter, json);
        doReturn(emitter).when(stream).createEmitter();
        try {
            stream.open();
            raw.put("snapshotId", SseEventCapture.id(2));
            cache.set(raw.toString());
            var notice = json.createObjectNode().put("schemaVersion", 1)
                    .put("previousSnapshotId", SseEventCapture.id(1)).put("snapshotId", SseEventCapture.id(2));
            notice.putArray("changedModules").add("coreIndices").add("marketFundFlow");
            stream.onMessage(SseEventCapture.message(notice), null);
            assertEquals("ready", events.get(0).path("event").asText());
            assertEquals("patch", events.get(1).path("event").asText());
            var patch = events.get(1).path("data");
            assertEquals(reader.publicSnapshotId(SseEventCapture.id(1)), patch.path("baseSnapshotId").asText());
            assertEquals(reader.getSnapshot().path("snapshotId"), patch.path("snapshotId"));
            assertTrue(patch.path("modules").path("coreIndices").path("tradeDate").isNull());
            assertTrue(patch.path("modules").path("marketFundFlow").path("data").path("series").isEmpty());
            ((ArrayNode) raw.path("modules").path("coreIndices").path("data").path("items").get(0)
                    .path("series")).addObject().put("collectedAt", "2026-10-01T21:00:00+08:00").put("price", 3000);
            cache.set(raw.toString());
            stream.onMessage(SseEventCapture.message(notice), null);
            assertEquals("resync", events.get(2).path("event").asText());
        } finally {
            stream.shutdown();
        }
    }

    private MarketIndexConfigService config() {
        var config = mock(MarketIndexConfigService.class);
        when(config.enabled()).thenReturn(List.of(new MarketIndexConfigDto("sh000001", "上证指数", true, 1)));
        return config;
    }

    private ObjectNode undatedSnapshot(String status) {
        var raw = rawSnapshot(true);
        var modules = raw.path("modules");
        for (String name : List.of("industrySectors", "conceptSectors", "marketFundFlow", "coreIndices")) {
            var module = (ObjectNode) modules.path(name);
            module.put("status", status).putNull("tradeDate");
            module.put("lastSuccessAt", "2026-10-01T21:00:00+08:00");
            module.put("lastAttemptAt", "2026-10-01T21:00:00+08:00");
            if (name.endsWith("Sectors")) {
                var data = module.putObject("data").put("source", "THS").put("period", "INTRADAY");
                data.putArray("items");
            }
        }
        for (var row : modules.path("coreIndices").path("data").path("items")) {
            ((ObjectNode) row).put("collectedAt", "2026-10-01T21:00:00+08:00");
            ((ArrayNode) row.path("series")).removeAll();
        }
        var fund = ((ObjectNode) modules.path("marketFundFlow")).putObject("data");
        fund.put("source", "THS_INDIVIDUAL_AGGREGATE");
        fund.putArray("series");
        fund.putObject("latest").put("collectedAt", "2026-10-01T21:00:00+08:00")
                .put("inflow", 100).put("outflow", 20).put("netAmount", 80)
                .put("riseCount", 2).put("fallCount", 3).put("flatCount", 1).put("stockCount", 6);
        return raw;
    }

    private MarketSnapshotServiceImpl service(String raw, MarketIndexConfigService config) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:market:v1:snapshot")).thenReturn(raw);
        return new MarketSnapshotServiceImpl(redis, json, config);
    }

    private ObjectNode rawSnapshot(boolean indices) {
        ObjectNode root = json.createObjectNode();
        root.put("schemaVersion", 1);
        root.put("provider", "akshare");
        root.put("snapshotId", "0123456789abcdef0123456789abcdef");
        root.put("generatedAt", "2026-09-30T10:00:00+08:00");
        ObjectNode modules = root.putObject("modules");
        for (String name : List.of("industrySectors", "conceptSectors", "marketFundFlow")) {
            ObjectNode module = modules.putObject(name);
            module.put("status", "ERROR");
            module.putNull("tradeDate");
            module.put("tradeDateBasis", "CALENDAR");
            module.putNull("lastSuccessAt");
            module.put("lastAttemptAt", "2026-09-30T10:00:00+08:00");
            module.putNull("message");
            module.putNull("data");
        }
        if (indices) {
            ObjectNode module = modules.putObject("coreIndices");
            module.put("status", "FRESH");
            module.put("tradeDate", "2026-09-30");
            module.put("tradeDateBasis", "CALENDAR");
            module.put("lastSuccessAt", "2026-09-30T10:00:00+08:00");
            module.put("lastAttemptAt", "2026-09-30T10:00:00+08:00");
            module.putNull("message");
            ObjectNode data = module.putObject("data");
            data.put("source", "SINA_INDEX");
            data.putNull("sourceTime");
            var items = data.putArray("items");
            for (String code : List.of("sh000001", "sz399001", "sh000300", "sz399006", "sh000688")) {
                ObjectNode item = items.addObject();
                item.put("code", code);
                item.put("name", code);
                item.put("price", 3000);
                for (String field : List.of("change", "changePercent", "previousClose", "open", "high",
                        "low", "volume", "amount")) item.putNull(field);
                item.putNull("sourceTime");
                item.put("collectedAt", "2026-09-30T10:00:00+08:00");
                ObjectNode point = item.putArray("series").addObject();
                point.put("collectedAt", "2026-09-30T10:00:00+08:00");
                point.put("price", 3000);
            }
        }
        return root;
    }
}
