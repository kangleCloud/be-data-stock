package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.vita.marketdata.support.SseEventCapture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EtfMonitorStreamServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicReference<ObjectNode> current = new AtomicReference<>();
    private EtfMonitorStreamService stream;
    private List<JsonNode> events;

    @BeforeEach
    void openStream() throws Exception {
        var dashboards = mock(EtfMonitorDashboardService.class);
        when(dashboards.dashboard()).thenAnswer(call -> current.get());
        current.set(dashboard(1));
        stream = spy(new EtfMonitorStreamService(dashboards, json, mock(ScheduledExecutorService.class)));
        var emitter = mock(SseEmitter.class);
        events = attach(emitter, json);
        doReturn(emitter).when(stream).createEmitter();
        stream.open();
    }

    @AfterEach
    void closeStream() {
        stream.shutdown();
    }

    @Test
    void explicitResyncMustNotBeDiscardedAsDuplicateEvenAtSameVersion() {
        var notice = notice(1, 1);
        notice.put("resync", true);
        stream.onMessage(message(notice), null);
        assertEquals(2, events.size());
        assertResync(1);
    }

    @Test
    void sequentialQuoteUpdatesPreserveSourceTimesAndOmitAllocationReport() {
        assertEquals(id(1), events.get(0).path("data").path("stateId").asText());
        for (int version = 2; version <= 3; version++) {
            var next = dashboard(version);
            current.set(next);
            var before = next.deepCopy();
            stream.onMessage(message(notice(version - 1, version, "SH510050")), null);
            var event = events.get(version - 1);
            assertEquals("patch", event.path("event").asText());
            var patch = event.path("data");
            assertEquals(id(version - 1), patch.path("baseStateId").asText());
            assertEquals(id(version), patch.path("stateId").asText());
            assertEquals(1, patch.path("etfs").size());
            var row = patch.path("etfs").get(0);
            assertFalse(row.has("assetAllocation"));
            assertEquals(next.path("etfs").get(0).path("quote"), row.path("quote"));
            assertEquals("HISTORICAL", row.path("dataStatus").asText());
            assertEquals(before, next);
        }
        stream.onMessage(message(notice(2, 3, "SH510050")), null);
        assertEquals(3, events.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"redisAhead", "gap", "missingBase", "missingNext", "removedSymbol", "duplicateSymbol"})
    void brokenVersionOrSymbolsRequireResyncAndStayResync(String scenario) {
        current.set(dashboard(3));
        var notice = notice(1, 3, "SH510050");
        switch (scenario) {
            case "redisAhead" -> notice.put("stateId", id(2));
            case "gap" -> notice.put("baseStateId", id(2));
            case "missingBase" -> notice.remove("baseStateId");
            case "missingNext" -> notice.remove("stateId");
            case "removedSymbol" -> notice.withArray("changedSymbols").add("SH510300");
            case "duplicateSymbol" -> notice.withArray("changedSymbols").add("SH510050");
            default -> fail("未知测试场景");
        }
        stream.onMessage(message(notice), null);
        assertResync(1);
        stream.onMessage(message(notice(1, 3, "SH510050")), null);
        assertResync(2);
    }

    private void assertResync(int index) {
        assertEquals("resync", events.get(index).path("event").asText());
        assertTrue(events.get(index).path("data").isEmpty());
    }

    private ObjectNode notice(int base, int next, String... symbols) {
        var notice = json.createObjectNode().put("baseStateId", id(base)).put("stateId", id(next));
        var changed = notice.putArray("changedSymbols");
        for (String symbol : symbols) changed.add(symbol);
        return notice;
    }

    private ObjectNode dashboard(int version) {
        var dashboard = json.createObjectNode().put("stateId", id(version));
        var etf = dashboard.putArray("etfs").addObject().put("symbol", "SH510050")
                .put("dataStatus", "HISTORICAL").put("effectiveTradeDate", "2026-09-28");
        etf.putObject("quote").put("price", version).putNull("sourceTime")
                .put("collectedAt", "2026-09-28T15:01:00+08:00").put("status", "STALE");
        etf.putObject("assetAllocation").put("requestedReportPeriod", "2026-06-30");
        return dashboard;
    }
}
