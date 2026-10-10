package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.support.SseEventCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EtfNullDateDashboardTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicReference<String> cache = new AtomicReference<>();
    private final AtomicReference<String> state = new AtomicReference<>(SseEventCapture.id(1));

    @ParameterizedTest
    @ValueSource(strings = {"FRESH", "STALE"})
    void undatedQuoteSurvivesGetWithDelayedStatusAndNoCurves(String status) throws Exception {
        var raw = snapshot();
        quote(raw).put("status", status);
        cache.set(raw.toString());
        var result = reader().dashboard();
        assertTrue(result.path("tradeDate").isNull());
        assertUndated(result.path("etfs").get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"undatedSnapshotWithPoint", "undatedQuoteWithHistory", "missingSeries",
            "missingDate", "invalidDate", "numericDate"})
    void rejectsUnknownDatePointsAndInvalidSnapshotDates(String scenario) throws Exception {
        var raw = snapshot();
        var item = (ObjectNode) raw.path("items").get(0);
        switch (scenario) {
            case "undatedSnapshotWithPoint", "undatedQuoteWithHistory" -> {
                if (scenario.equals("undatedQuoteWithHistory")) raw.put("tradeDate", "2026-09-30");
                ((ArrayNode) item.path("priceSeries")).addObject()
                        .put("collectedAt", "2026-09-30T10:00:00+08:00").put("price", 2.4);
            }
            case "missingSeries" -> item.remove("priceSeries");
            case "missingDate" -> raw.remove("tradeDate");
            case "invalidDate" -> raw.put("tradeDate", "2026-99-99");
            case "numericDate" -> raw.put("tradeDate", 20261001);
            default -> fail("未知测试场景");
        }
        cache.set(raw.toString());
        assertEquals(503, assertThrows(ServiceException.class, () -> reader().dashboard()).getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"source", "price", "time", "missingDate", "invalidDate", "sourceTime", "nonFinitePrice"})
    void invalidQuoteRetainsExistingNoDataFallback(String scenario) throws Exception {
        var raw = snapshot();
        var quote = quote(raw);
        switch (scenario) {
            case "source" -> quote.put("source", "OTHER");
            case "price" -> quote.put("price", 0);
            case "time" -> quote.put("collectedAt", "2026-10-01T21:00:00");
            case "missingDate" -> quote.remove("tradeDate");
            case "invalidDate" -> quote.put("tradeDate", "invalid");
            case "sourceTime" -> quote.put("sourceTime", "2026-10-01T21:00:00+08:00");
            case "nonFinitePrice" -> quote.put("price", 2.5);
            default -> fail("未知测试场景");
        }
        cache.set(scenario.equals("nonFinitePrice") ? raw.toString().replace("2.5", "1e999") : raw.toString());
        var row = reader().dashboard().path("etfs").get(0);
        assertTrue(row.path("quote").isNull());
        assertEquals("NO_DATA", row.path("dataStatus").asText());
        assertTrue(row.path("series").isEmpty());
    }

    @Test
    void knownDateKeepsCurrentQuoteAndFiltersOlderHistory() throws Exception {
        var raw = snapshot();
        String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        raw.put("tradeDate", today);
        quote(raw).put("tradeDate", today).put("collectedAt", today + "T10:00:00+08:00");
        var series = (ArrayNode) raw.path("items").get(0).path("priceSeries");
        series.addObject().put("collectedAt", "2026-09-30T10:00:00+08:00").put("price", 2.4);
        series.addObject().put("collectedAt", today + "T10:00:00+08:00").put("price", 2.5);
        cache.set(raw.toString());
        var row = reader().dashboard().path("etfs").get(0);
        assertEquals("CURRENT", row.path("dataStatus").asText());
        assertEquals(today, row.path("effectiveTradeDate").asText());
        assertEquals(1, row.path("series").size());
        assertEquals(today + "T10:00:00+08:00", row.path("series").get(0).path("collectedAt").asText());
    }

    @Test
    void realReaderPublishesUndatedSsePatchAndResyncsOnHistory() throws Exception {
        var raw = snapshot();
        cache.set(raw.toString());
        var reader = reader();
        var stream = spy(new EtfMonitorStreamService(reader, json, mock(ScheduledExecutorService.class)));
        var emitter = mock(SseEmitter.class);
        var events = SseEventCapture.attach(emitter, json);
        doReturn(emitter).when(stream).createEmitter();
        try {
            stream.open();
            state.set(SseEventCapture.id(2));
            var notice = json.createObjectNode().put("baseStateId", SseEventCapture.id(1))
                    .put("stateId", SseEventCapture.id(2));
            notice.putArray("changedSymbols").add("SH510050");
            stream.onMessage(SseEventCapture.message(notice), null);
            assertEquals("ready", events.get(0).path("event").asText());
            assertEquals("patch", events.get(1).path("event").asText());
            var patch = events.get(1).path("data");
            assertEquals(SseEventCapture.id(1), patch.path("baseStateId").asText());
            assertEquals(reader.dashboard().path("stateId"), patch.path("stateId"));
            assertUndated(patch.path("etfs").get(0));
            ((ArrayNode) raw.path("items").get(0).path("priceSeries")).addObject()
                    .put("collectedAt", "2026-09-30T10:00:00+08:00").put("price", 2.4);
            cache.set(raw.toString());
            stream.onMessage(SseEventCapture.message(notice), null);
            assertEquals("resync", events.get(2).path("event").asText());
        } finally {
            stream.shutdown();
        }
    }

    private void assertUndated(com.fasterxml.jackson.databind.JsonNode row) {
        assertEquals(2.5, row.path("quote").path("price").asDouble());
        assertTrue(row.path("quote").path("tradeDate").isNull());
        assertEquals("2026-10-01T21:00:00+08:00", row.path("quote").path("collectedAt").asText());
        assertTrue(row.path("quote").path("sourceTime").isNull());
        assertTrue(row.path("effectiveTradeDate").isNull());
        assertEquals("DELAYED", row.path("dataStatus").asText());
        assertTrue(row.path("series").isEmpty());
        assertTrue(row.path("fundSeries").isEmpty());
    }

    private ObjectNode snapshot() throws Exception {
        return (ObjectNode) json.readTree("""
                {"schemaVersion":1,"source":"AKShare.fund_etf_category_sina","tradeDate":null,
                 "items":[{"symbol":"SH510050","quote":{"source":"SINA_ETF","sourceTime":null,
                 "collectedAt":"2026-10-01T21:00:00+08:00","tradeDate":null,"price":2.5,"status":"FRESH"},
                 "priceSeries":[]}]}
                """);
    }

    private ObjectNode quote(ObjectNode raw) {
        return (ObjectNode) raw.path("items").get(0).path("quote");
    }

    private EtfMonitorDashboardService reader() {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:etf-monitor:v1:state-id")).thenAnswer(call -> state.get());
        when(values.get("stock:etf-monitor:v1:snapshot")).thenAnswer(call -> cache.get());
        when(values.get("stock:etf-monitor:v1:enabled")).thenReturn("""
                [{"symbol":"SH510050","code":"510050","name":"50ETF","market":"SH"}]
                """);
        return new EtfMonitorDashboardService(redis, json, mock(EtfMonitorProfileMapper.class),
                mock(EtfAssetAllocationReportMapper.class), new StockMonitorProperty());
    }
}
