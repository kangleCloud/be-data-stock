package com.vita.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.controller.market.MarketDashboardController;
import com.vita.core.StreamEndpoint;
import com.vita.core.exception.ControllerExceptionHandler;
import com.vita.core.exception.ServiceException;
import com.vita.market.service.MarketSnapshotService;
import com.vita.market.service.MarketSnapshotStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MarketSnapshotControllerContractTest {

    @Test
    void anonymousSnapshotKeepsOriginalResponseShape() throws Exception {
        assertThat(MarketDashboardController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/market/dashboard");
        var method = MarketDashboardController.class.getMethod("getSnapshot");
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/snapshot");

        MarketSnapshotService service = mock(MarketSnapshotService.class);
        when(service.getSnapshot()).thenReturn(new ObjectMapper().readTree(
                "{\"schemaVersion\":1,\"modules\":{\"marketFundFlow\":{\"tradeDate\":\"2026-09-22\"}}}"));
        MockMvcBuilders.standaloneSetup(new MarketDashboardController(
                service, mock(MarketSnapshotStreamService.class))).build()
                .perform(get("/openapi/api/market/dashboard/snapshot").contextPath("/openapi/api")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.schemaVersion").value(1))
                .andExpect(jsonPath("$.content.modules.marketFundFlow.tradeDate")
                        .value("2026-09-22"));
    }

    @Test
    void streamSendsInitialV1SnapshotFrame() throws Exception {
        var method = MarketDashboardController.class.getMethod("stream");
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/stream");
        assertThat(method.isAnnotationPresent(StreamEndpoint.class)).isTrue();

        MarketSnapshotService service = mock(MarketSnapshotService.class);
        when(service.getSnapshot()).thenReturn(new ObjectMapper().readTree(
                "{\"schemaVersion\":1,\"modules\":{}}"));
        MarketSnapshotStreamService streamService = new MarketSnapshotStreamService(service);
        try {
            MvcResult result = MockMvcBuilders.standaloneSetup(
                    new MarketDashboardController(service, streamService)).build()
                    .perform(get("/openapi/api/market/dashboard/stream").contextPath("/openapi/api")
                            .accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            assertThat(result.getResponse().getContentType()).startsWith("text/event-stream");
            assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
            assertThat(result.getResponse().getContentAsString())
                    .contains("event:snapshot")
                    .contains("data:{\"schemaVersion\":1,\"modules\":{}}");
        } finally {
            streamService.shutdown();
        }
    }

    @Test
    void streamHandshakeReturns404Or503BeforeSseHeaders() throws Exception {
        MarketSnapshotService service = mock(MarketSnapshotService.class);
        MarketSnapshotStreamService streamService = mock(MarketSnapshotStreamService.class);
        for (int code : new int[]{404, 503}) {
            reset(streamService);
            when(streamService.open()).thenThrow(new ServiceException(code, "快照不可用"));
            MvcResult result = MockMvcBuilders.standaloneSetup(
                    new MarketDashboardController(service, streamService))
                    .setControllerAdvice(new ControllerExceptionHandler(mock(ObjectProvider.class)))
                    .build().perform(get("/openapi/api/market/dashboard/stream")
                            .contextPath("/openapi/api").accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().is(code)).andReturn();
            assertThat(result.getResponse().getContentType()).startsWith("application/json");
            assertThat(result.getResponse().getContentAsString()).contains("\"code\":" + code);
        }
    }
}
