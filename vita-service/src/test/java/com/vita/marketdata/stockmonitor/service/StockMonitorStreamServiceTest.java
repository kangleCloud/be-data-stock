package com.vita.marketdata.stockmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.marketdata.stockmonitor.dto.StockMonitorDtos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.vita.marketdata.support.SseEventCapture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockMonitorStreamServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicReference<StockMonitorDtos.Dashboard> current = new AtomicReference<>();
    private StockMonitorStreamService stream;
    private List<JsonNode> events;

    @BeforeEach
    void openStream() throws Exception {
        var monitor = mock(StockMonitorService.class);
        when(monitor.dashboard()).thenAnswer(call -> current.get());
        current.set(dashboard(1, stock("SH600000", 10, List.of()), stock("SZ000001", 20, List.of())));
        stream = spy(new StockMonitorStreamService(monitor, json, mock(ScheduledExecutorService.class)));
        var emitter = mock(SseEmitter.class);
        events = attach(emitter, json);
        doReturn(emitter).when(stream).createEmitter();
        stream.open();
        assertEquals(id(1), events.get(0).path("data").path("stateId").asText());
    }

    @AfterEach
    void closeStream() {
        stream.shutdown();
    }

    @Test
    void individuallyCompletedQuotesAndFundsKeepContinuousVersionsAndOldSourceTimes() throws Exception {
        var first = stock("SH600000", 10, List.of());
        var second = stock("SZ000001", 21, List.of());
        current.set(dashboard(2, first, second));
        stream.onMessage(message(notice(1, 2, "SZ000001")), null);
        first = stock("SH600000", 11, List.of());
        current.set(dashboard(3, first, second));
        stream.onMessage(message(notice(2, 3, "SH600000")), null);
        var point = new StockMonitorDtos.FundPoint("2026-09-28T15:10:00+08:00",
                new BigDecimal("100"), new BigDecimal("40"), new BigDecimal("60"));
        current.set(dashboard(4, stock("SH600000", 11, List.of(point)), second));
        stream.onMessage(message(notice(3, 4, "SH600000")), null);
        stream.onMessage(message(notice(3, 4, "SH600000")), null);
        assertEquals(4, events.size(), "同版本重复通知不重复推送");
        for (int i = 1; i <= 3; i++) {
            var patch = events.get(i);
            assertEquals("patch", patch.path("event").asText());
            assertEquals(id(i), patch.path("data").path("baseStateId").asText());
            assertEquals(id(i + 1), patch.path("data").path("stateId").asText());
            assertEquals(1, patch.path("data").path("stocks").size());
        }
        assertEquals("SZ000001", events.get(1).path("data").path("stocks").get(0).path("symbol").asText());
        var payload = events.get(3).path("data").path("stocks").get(0);
        assertEquals(json.readTree(json.writeValueAsString(first.quote())), payload.path("quote"));
        assertEquals(json.readTree(json.writeValueAsString(first.series())), payload.path("series"));
        assertEquals("HISTORICAL", payload.path("dataStatus").asText());
        assertEquals(first.effectiveTradeDate(), payload.path("effectiveTradeDate").asText());
        assertTrue(payload.path("closeConfirmed").asBoolean());
        assertEquals(1, payload.path("fundSeries").size());
        var fund = payload.path("fundSeries").get(0);
        assertEquals(point.collectedAt(), fund.path("collectedAt").asText());
        assertEquals(0, point.inflow().compareTo(fund.path("inflow").decimalValue()));
        assertEquals(0, point.outflow().compareTo(fund.path("outflow").decimalValue()));
        assertEquals(0, point.netAmount().compareTo(fund.path("netAmount").decimalValue()));
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
    void redisAheadOfNoticeRequiresResyncUntilReconnect() {
        current.set(dashboard(3, stock("SH600000", 13, List.of())));
        stream.onMessage(message(notice(1, 2, "SH600000")), null);
        assertResync(1);
        stream.onMessage(message(notice(1, 3, "SH600000")), null);
        assertResync(2);
    }

    @Test
    void skippedBaseVersionRequiresResync() {
        current.set(dashboard(3, stock("SH600000", 13, List.of())));
        stream.onMessage(message(notice(2, 3, "SH600000")), null);
        assertResync(1);
    }

    @Test
    void outOfOrderNoticeAfterSuccessfulPatchRequiresResync() {
        current.set(dashboard(2, stock("SH600000", 12, List.of())));
        stream.onMessage(message(notice(1, 2, "SH600000")), null);
        assertEquals("patch", events.get(1).path("event").asText());
        stream.onMessage(message(notice(0, 1, "SH600000")), null);
        assertResync(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missingBase", "missingState", "duplicateSymbols"})
    void missingBaseOrStateAndDuplicateSymbolsRequireResync(String scenario) {
        current.set(dashboard(2, stock("SH600000", 12, List.of())));
        var notice = notice(1, 2, "SH600000");
        switch (scenario) {
            case "missingBase" -> notice.remove("baseStateId");
            case "missingState" -> notice.remove("stateId");
            case "duplicateSymbols" -> notice.withArray("changedSymbols").add("SH600000");
            default -> fail("未知测试场景");
        }
        stream.onMessage(message(notice), null);
        assertResync(1);
    }

    @Test
    void removedStockCannotProduceIncompletePatch() {
        current.set(dashboard(2, stock("SH600000", 12, List.of())));
        stream.onMessage(message(notice(1, 2, "SZ000001")), null);
        assertResync(1);
    }

    @Test
    void concurrentFundsAndQuotesWithLatestRedisVersionCannotInventBasePatch() throws Exception {
        current.set(dashboard(3, stock("SH600000", 13, List.of())));
        var workers = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var funds = workers.submit(() -> { start.await(); stream.onMessage(message(notice(1, 2, "SH600000")), null); return null; });
            var quote = workers.submit(() -> { start.await(); stream.onMessage(message(notice(2, 3, "SH600000")), null); return null; });
            start.countDown();
            funds.get(5, TimeUnit.SECONDS);
            quote.get(5, TimeUnit.SECONDS);
            assertEquals(3, events.size());
            assertResync(1);
            assertResync(2);
        } finally {
            workers.shutdownNow();
        }
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

    private StockMonitorDtos.Dashboard dashboard(int version, StockMonitorDtos.Stock... stocks) {
        return new StockMonitorDtos.Dashboard(1, id(version), true, "2026-09-28", List.of(stocks));
    }

    private StockMonitorDtos.Stock stock(String symbol, int price, List<StockMonitorDtos.FundPoint> funds) {
        var quote = new StockMonitorDtos.Quote("XQ", "2026-09-28T15:01:00+08:00",
                "2026-09-28T15:02:00+08:00", "2026-09-28", BigDecimal.valueOf(price),
                null, null, null, null, null, null, null, null, null, null, "STALE");
        return new StockMonitorDtos.Stock(symbol, symbol.substring(2), "测试股票", symbol.substring(0, 2),
                1, null, quote, List.of(new StockMonitorDtos.SeriesPoint(quote.sourceTime(), quote.price())),
                "2026-09-28", "HISTORICAL", true, funds);
    }
}
