package com.vita.stockmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.test.RecordingSseEmitter;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockMonitorStreamContractTest {
    private static final String FIRST = "0123456789abcdef0123456789abcdef";
    private static final String SECOND = "fedcba9876543210fedcba9876543210";

    @Test
    @SuppressWarnings("unchecked")
    void legacyStateStartsWithJsonNullAndRequiresResync() {
        StockMonitorService monitor = mock(StockMonitorService.class);
        when(monitor.dashboard()).thenReturn(dashboard(null), dashboard(SECOND));
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        StockMonitorStreamService stream = new StockMonitorStreamService(monitor, new ObjectMapper(), scheduler) {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };
        try {
            stream.open();
            String notice = "{\"baseStateId\":null,\"stateId\":\"" + SECOND
                    + "\",\"changedSymbols\":[\"SH600000\"]}";
            stream.onMessage(new DefaultMessage("stock:monitor:v1:updates".getBytes(StandardCharsets.UTF_8),
                    notice.getBytes(StandardCharsets.UTF_8)), null);

            assertThat(emitter.events().get(0)).contains("event:ready", "\"stateId\":null")
                    .doesNotContain("\"stateId\":\"null\"");
            assertThat(emitter.events().get(1)).contains("event:resync");
        } finally {
            stream.shutdown();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void readyAndVersionedPatchOnlyCarryChangedStock() {
        StockMonitorService monitor = mock(StockMonitorService.class);
        when(monitor.dashboard()).thenReturn(dashboard(FIRST), dashboard(SECOND));
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        StockMonitorStreamService stream = new StockMonitorStreamService(monitor, new ObjectMapper(), scheduler) {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };
        try {
            stream.open();
            notify(stream, FIRST, SECOND, false);

            assertThat(emitter.events()).hasSize(2);
            assertThat(emitter.events().get(0)).contains("event:ready", FIRST).doesNotContain("stocks");
            assertThat(emitter.events().get(1)).contains("event:patch", SECOND, "SH600000")
                    .doesNotContain("SZ000001");
        } finally {
            stream.shutdown();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void gapAndConfigChangeRequireResync() {
        StockMonitorService monitor = mock(StockMonitorService.class);
        when(monitor.dashboard()).thenReturn(dashboard(FIRST), dashboard(SECOND), dashboard(SECOND));
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        StockMonitorStreamService stream = new StockMonitorStreamService(monitor, new ObjectMapper(), scheduler) {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };
        try {
            stream.open();
            notify(stream, SECOND, SECOND, false);
            notify(stream, FIRST, SECOND, true);

            assertThat(emitter.events().get(1)).contains("event:resync");
            assertThat(emitter.events().get(2)).contains("event:resync");
            assertThat(emitter.events()).noneMatch(event -> event.contains("event:patch"));
        } finally {
            stream.shutdown();
        }
    }

    private StockMonitorDtos.Dashboard dashboard(String stateId) {
        StockMonitorDtos.Stock stock = new StockMonitorDtos.Stock("SH600000", "600000", "浦发银行", "SH", 1,
                null, null, List.of(), null, "NO_DATA", false, List.of());
        return new StockMonitorDtos.Dashboard(1, stateId, true, null, List.of(stock));
    }

    private void notify(StockMonitorStreamService stream, String base, String next, boolean resync) {
        String notice = "{\"baseStateId\":\"" + base + "\",\"stateId\":\"" + next
                + "\",\"changedSymbols\":[\"SH600000\"],\"resync\":" + resync + "}";
        stream.onMessage(new DefaultMessage("stock:monitor:v1:updates".getBytes(StandardCharsets.UTF_8),
                notice.getBytes(StandardCharsets.UTF_8)), null);
    }

}
