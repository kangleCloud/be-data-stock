package com.vita.config;

import cn.dev33.satoken.stp.StpLogic;
import com.vita.auth.config.SaTokenConfigure;
import com.vita.auth.property.AuthProperty;
import com.vita.controller.stockmonitor.StockMonitorLocalRefreshController;
import com.vita.controller.stockmonitor.StockMonitorRefreshController;
import com.vita.core.exception.ControllerExceptionHandler;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.StockMonitorRefreshService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockMonitorLocalRefreshHttpTest {
    @Test
    void localManualEndpointsNeedNoTokenButRejectRemoteClients() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestConfiguration.class);
            context.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            StockMonitorRefreshService service = context.getBean(StockMonitorRefreshService.class);
            StockMonitorDtos.RefreshStatus result = new StockMonitorDtos.RefreshStatus(
                    true, "job", "SUCCESS", null, null, null);
            when(service.refreshDictionary()).thenReturn(result);
            when(service.refreshProfiles()).thenReturn(result);

            mvc.perform(post("/scheduler/api/local/stock-monitor/v1/dictionary/refresh")
                            .contextPath("/scheduler/api").with(request -> {
                                request.setRemoteAddr("127.0.0.1");
                                return request;
                            }))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.status").value("SUCCESS"));
            mvc.perform(post("/scheduler/api/local/stock-monitor/v1/profiles/refresh")
                            .contextPath("/scheduler/api").with(request -> {
                                request.setRemoteAddr("0:0:0:0:0:0:0:1");
                                return request;
                            }))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.status").value("SUCCESS"));
            mvc.perform(post("/scheduler/api/local/stock-monitor/v1/dictionary/refresh")
                            .contextPath("/scheduler/api").header("X-Forwarded-For", "127.0.0.1")
                            .with(request -> {
                                request.setRemoteAddr("192.0.2.10");
                                return request;
                            }))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
            mvc.perform(post("/scheduler/api/local/stock-monitor/v1/dictionary/refresh")
                            .contextPath("/scheduler/api").header("Forwarded", "for=192.0.2.10")
                            .with(request -> {
                                request.setRemoteAddr("127.0.0.1");
                                return request;
                            }))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
            verify(service, times(1)).refreshDictionary();
            verify(service, times(1)).refreshProfiles();

            mvc.perform(post("/scheduler/api/internal/stock-monitor/v1/refresh")
                            .contextPath("/scheduler/api"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
            verify(service, never()).refresh();
            verifyNoInteractions(context.getBean(StpLogic.class));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @org.springframework.context.annotation.Import({SaTokenConfigure.class,
            ControllerExceptionHandler.class, StockMonitorLocalRefreshController.class,
            StockMonitorRefreshController.class, StockMonitorInternalAuthConfiguration.class})
    static class TestConfiguration {
        @Bean
        AuthProperty authProperty() {
            return new AuthProperty();
        }

        @Bean
        StpLogic stpLogic() {
            return mock(StpLogic.class);
        }

        @Bean
        RequestTraceInterceptor requestTraceInterceptor() throws Exception {
            RequestTraceInterceptor interceptor = mock(RequestTraceInterceptor.class);
            when(interceptor.preHandle(any(), any(), any())).thenReturn(true);
            return interceptor;
        }

        @Bean
        StockMonitorRefreshService refreshService() {
            return mock(StockMonitorRefreshService.class);
        }

        @Bean
        StockMonitorProperty stockMonitorProperty() {
            StockMonitorProperty property = new StockMonitorProperty();
            property.setInternalToken("secret");
            return property;
        }
    }
}
