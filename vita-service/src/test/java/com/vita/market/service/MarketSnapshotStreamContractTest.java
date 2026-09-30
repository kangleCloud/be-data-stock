package com.vita.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class MarketSnapshotStreamContractTest {
    @Test
    @SuppressWarnings("unchecked")
    void initialEventAndRedisNotificationBothReadTheCompleteSnapshot() throws Exception {
        MarketSnapshotService snapshots = mock(MarketSnapshotService.class);
        ObjectMapper mapper = new ObjectMapper();
        when(snapshots.getSnapshot()).thenReturn(
                mapper.readTree("{\"schemaVersion\":1,\"modules\":{\"industrySectors\":{}}}"),
                mapper.readTree("{\"schemaVersion\":1,\"modules\":{\"industrySectors\":{\"status\":\"STALE\"}}}"));
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        RecordingEmitter emitter = new RecordingEmitter();
        MarketSnapshotStreamService stream = new MarketSnapshotStreamService(snapshots, scheduler) {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };
        try {
            assertThat(stream.open()).isSameAs(emitter);
            assertThat(stream.activeClientCount()).isEqualTo(1);

            stream.onMessage(mock(Message.class), null);

            verify(snapshots, times(2)).getSnapshot();
            assertThat(emitter.events).hasSize(2);
            assertThat(emitter.events).allSatisfy(event -> assertThat(event).contains("event:snapshot"));
            assertThat(emitter.events.get(0)).contains("industrySectors");
            assertThat(emitter.events.get(1)).contains("STALE");
        } finally {
            stream.shutdown();
        }
    }

    private static final class RecordingEmitter extends SseEmitter {
        private final List<String> events = new ArrayList<>();

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            events.add(builder.build().stream().map(value -> String.valueOf(value.getData()))
                    .reduce("", String::concat));
        }
    }
}
