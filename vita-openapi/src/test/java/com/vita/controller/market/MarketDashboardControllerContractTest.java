package com.vita.controller.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.config.market.MarketDashboardPublicAuthConfiguration;
import com.vita.market.service.MarketSnapshotService;
import com.vita.market.service.MarketSnapshotStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MarketDashboardControllerContractTest {
    @Test
    void getReturnsTheUnchangedSnapshot() throws Exception {
        MarketSnapshotService snapshots = mock(MarketSnapshotService.class);
        MarketSnapshotStreamService stream = mock(MarketSnapshotStreamService.class);
        JsonNode snapshot = new ObjectMapper().readTree("{\"schemaVersion\":1,\"modules\":{}}");
        when(snapshots.getSnapshot()).thenReturn(snapshot);

        assertThat(new MarketDashboardController(snapshots, stream).getSnapshot().getContent()).isSameAs(snapshot);
        verify(snapshots).getSnapshot();
    }

    @Test
    void streamDelegatesToCacheBackedServiceAndPathsRemainExact() throws Exception {
        MarketSnapshotService snapshots = mock(MarketSnapshotService.class);
        MarketSnapshotStreamService stream = mock(MarketSnapshotStreamService.class);
        SseEmitter emitter = new SseEmitter();
        when(stream.open()).thenReturn(emitter);

        ResponseEntity<SseEmitter> response = new MarketDashboardController(snapshots, stream).stream();

        assertThat(response.getBody()).isSameAs(emitter);
        verify(stream).open();
        assertThat(MarketDashboardController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/market/dashboard");
        Method get = MarketDashboardController.class.getMethod("getSnapshot");
        Method sse = MarketDashboardController.class.getMethod("stream");
        assertThat(get.getAnnotation(GetMapping.class).value()).containsExactly("/snapshot");
        assertThat(sse.getAnnotation(GetMapping.class).value()).containsExactly("/stream");
    }

    @Test
    void anonymousBoundaryOnlyContainsSnapshotAndStream() {
        List<String> paths = new MarketDashboardPublicAuthConfiguration().marketDashboardPublicPaths().paths();
        assertThat(paths).containsExactly("/market/dashboard/snapshot", "/market/dashboard/stream");
    }
}
