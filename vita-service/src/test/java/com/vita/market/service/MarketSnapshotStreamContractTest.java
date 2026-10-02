package com.vita.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.test.RecordingSseEmitter;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class MarketSnapshotStreamContractTest {
    @Test
    @SuppressWarnings("unchecked")
    void legacySnapshotOpensWithNullVersionAndRequiresResync() throws Exception {
        MarketSnapshotService snapshots = mock(MarketSnapshotService.class);
        ObjectMapper mapper = new ObjectMapper();
        String next = "fedcba9876543210fedcba9876543210";
        when(snapshots.getSnapshot()).thenReturn(mapper.readTree("{\"snapshotId\":null}"),
                mapper.readTree("{\"snapshotId\":\"" + next + "\",\"modules\":{}}"));
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        MarketSnapshotStreamService stream = new MarketSnapshotStreamService(snapshots, mapper, scheduler) {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };
        try {
            stream.open();
            String notice = "{\"schemaVersion\":1,\"snapshotId\":\"" + next
                    + "\",\"previousSnapshotId\":null,\"changedModules\":[]}";
            stream.onMessage(new DefaultMessage("stock:market:v1:updates".getBytes(StandardCharsets.UTF_8),
                    notice.getBytes(StandardCharsets.UTF_8)), null);

            assertThat(emitter.events().get(0)).contains("event:ready", "\"snapshotId\":null")
                    .doesNotContain("\"snapshotId\":\"null\"");
            assertThat(emitter.events().get(1)).contains("event:resync");
        } finally {
            stream.shutdown();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void readyCarriesVersionAndNotificationOnlySendsChangedModule() throws Exception {
        MarketSnapshotService snapshots = mock(MarketSnapshotService.class);
        ObjectMapper mapper = new ObjectMapper();
        String first = "0123456789abcdef0123456789abcdef";
        String second = "fedcba9876543210fedcba9876543210";
        when(snapshots.getSnapshot()).thenReturn(
                mapper.readTree("{\"snapshotId\":\"" + first + "\"}"),
                mapper.readTree("{\"snapshotId\":\"" + second + "\",\"generatedAt\":\"2026-09-30T10:02:00+08:00\","
                        + "\"modules\":{\"industrySectors\":{\"status\":\"STALE\"},\"conceptSectors\":{\"status\":\"FRESH\"}}}"));
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        MarketSnapshotStreamService stream = new MarketSnapshotStreamService(snapshots, mapper, scheduler) {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };
        try {
            assertThat(stream.open()).isSameAs(emitter);
            assertThat(stream.activeClientCount()).isEqualTo(1);

            String notice = "{\"schemaVersion\":1,\"snapshotId\":\"" + second
                    + "\",\"previousSnapshotId\":\"" + first
                    + "\",\"changedModules\":[\"industrySectors\"]}";
            stream.onMessage(new DefaultMessage("stock:market:v1:updates".getBytes(StandardCharsets.UTF_8),
                    notice.getBytes(StandardCharsets.UTF_8)), null);
            stream.onMessage(new DefaultMessage("stock:market:v1:updates".getBytes(StandardCharsets.UTF_8),
                    notice.getBytes(StandardCharsets.UTF_8)), null);

            verify(snapshots, times(3)).getSnapshot();
            assertThat(emitter.events()).hasSize(2);
            assertThat(emitter.events().get(0)).contains("event:ready", first).doesNotContain("modules");
            assertThat(emitter.events().get(1)).contains("event:patch", "industrySectors", "STALE")
                    .doesNotContain("conceptSectors");
        } finally {
            stream.shutdown();
        }
    }

}
