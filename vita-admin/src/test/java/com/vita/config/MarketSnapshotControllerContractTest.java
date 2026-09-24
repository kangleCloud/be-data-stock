package com.vita.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.auth.handler.SaTokenExceptionHandler;
import com.vita.controller.market.MarketDashboardController;
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
import org.springframework.web.servlet.HandlerInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MarketSnapshotControllerContractTest {

    @Test
    void endpointUsesExpectedPathAndPermission() throws Exception {
        assertThat(MarketDashboardController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/market/dashboard");
        var method = MarketDashboardController.class.getMethod("getSnapshot");
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/snapshot");
        assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("market:dashboard:view");

        MarketSnapshotService service = mock(MarketSnapshotService.class);
        when(service.getSnapshot()).thenReturn(new ObjectMapper().readTree(
                "{\"schemaVersion\":1,\"modules\":{\"marketFundFlow\":{\"tradeDate\":\"2026-09-22\"}}}"));
        MockMvcBuilders.standaloneSetup(new MarketDashboardController(service, mock(MarketSnapshotStreamService.class))).build()
                .perform(get("/market/dashboard/snapshot").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.schemaVersion").value(1))
                .andExpect(jsonPath("$.content.modules.marketFundFlow.tradeDate")
                        .value("2026-09-22"));
    }

    @Test
    void streamDeclaresPermissionAndSendsInitialSnapshotFrame() throws Exception {
        var method = MarketDashboardController.class.getMethod("stream");
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/stream");
        assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("market:dashboard:view");

        MarketSnapshotService service = mock(MarketSnapshotService.class);
        when(service.getSnapshot()).thenReturn(new ObjectMapper().readTree(
                "{\"schemaVersion\":1,\"modules\":{}}"));
        MarketSnapshotStreamService streamService = new MarketSnapshotStreamService(service);
        try {
            MvcResult result = MockMvcBuilders.standaloneSetup(
                    new MarketDashboardController(service, streamService)).build()
                    .perform(get("/admin/api/market/dashboard/stream").contextPath("/admin/api")
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
    void streamHandshakeReturnsHttpErrorBeforeSseHeaders() throws Exception {
        MarketSnapshotService service = mock(MarketSnapshotService.class);
        MarketSnapshotStreamService streamService = mock(MarketSnapshotStreamService.class);
        for (int code : new int[]{404, 503}) {
            reset(streamService);
            when(streamService.open()).thenThrow(new ServiceException(code, "快照不可用"));
            MvcResult result = MockMvcBuilders.standaloneSetup(
                    new MarketDashboardController(service, streamService))
                    .setControllerAdvice(new ControllerExceptionHandler(mock(ObjectProvider.class)))
                    .build().perform(get("/admin/api/market/dashboard/stream").contextPath("/admin/api")
                            .accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().is(code)).andReturn();
            assertThat(result.getResponse().getContentType()).startsWith("application/json");
            assertThat(result.getResponse().getContentAsString()).contains("\"code\":" + code);
        }
    }

    @Test
    void streamPermissionFailureReturnsHttp403() throws Exception {
        MarketSnapshotService service = mock(MarketSnapshotService.class);
        MarketSnapshotStreamService streamService = mock(MarketSnapshotStreamService.class);
        HandlerInterceptor deny = new HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response, Object handler) {
                throw mock(NotPermissionException.class);
            }
        };
        MvcResult result = MockMvcBuilders.standaloneSetup(
                new MarketDashboardController(service, streamService))
                .addInterceptors(deny)
                .setControllerAdvice(new SaTokenExceptionHandler())
                .build().perform(get("/admin/api/market/dashboard/stream").contextPath("/admin/api")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isForbidden()).andReturn();
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
    }

    @Test
    void streamLoginFailureReturnsHttp401() throws Exception {
        HandlerInterceptor deny = new HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response, Object handler) {
                throw mock(NotLoginException.class);
            }
        };
        MvcResult result = MockMvcBuilders.standaloneSetup(new MarketDashboardController(
                mock(MarketSnapshotService.class), mock(MarketSnapshotStreamService.class)))
                .addInterceptors(deny)
                .setControllerAdvice(new SaTokenExceptionHandler())
                .build().perform(get("/admin/api/market/dashboard/stream").contextPath("/admin/api")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized()).andReturn();
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":401");
    }

}
