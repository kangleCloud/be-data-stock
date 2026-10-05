package com.vita.marketdata.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.market.dto.MarketIndexConfigDto;
import com.vita.marketdata.market.service.impl.MarketSnapshotServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketCoreIndicesContractTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void acceptsFiveSinaRowsButPublishesOnlyConfiguredOrderAndVersion() {
        ObjectNode raw = rawSnapshot(true);
        MarketIndexConfigService config = mock(MarketIndexConfigService.class);
        when(config.enabled()).thenReturn(List.of(
                new MarketIndexConfigDto("sz399006", "创业板指", true, 1),
                new MarketIndexConfigDto("sh000001", "上证指数", true, 2)));
        var result = service(raw.toString(), config).getSnapshot();
        var visible = result.path("modules").path("coreIndices").path("data").path("items");
        assertEquals(2, visible.size());
        assertEquals("sz399006", visible.get(0).path("code").asText());
        assertEquals("sh000001", visible.get(1).path("code").asText());
        assertNotEquals(raw.path("snapshotId").asText(), result.path("snapshotId").asText());
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
