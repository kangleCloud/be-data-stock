package com.vita.config;

import cn.dev33.satoken.stp.StpLogic;
import com.vita.auth.config.SaTokenConfigure;
import com.vita.auth.property.AuthProperty;
import com.vita.config.stockmonitor.StockMonitorPublicAuthConfiguration;
import com.vita.controller.stockmonitor.StockMonitorDashboardController;
import com.vita.core.CommonResult;
import com.vita.core.exception.ControllerExceptionHandler;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.service.StockMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockMonitorPublicAuthTest {
    @Test
    void onlyTheDashboardPathIsAnonymous() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestConfiguration.class);
            context.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(get("/openapi/api/stock-monitor/v1/dashboard").contextPath("/openapi/api"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.schemaVersion").value(1));
            mvc.perform(get("/openapi/api/private/probe").contextPath("/openapi/api"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
            verify(context.getBean(StpLogic.class)).checkLogin();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @org.springframework.context.annotation.Import({SaTokenConfigure.class,
            ControllerExceptionHandler.class, StockMonitorDashboardController.class,
            StockMonitorPublicAuthConfiguration.class, PrivateProbeController.class})
    static class TestConfiguration {
        @Bean
        AuthProperty authProperty() {
            return new AuthProperty();
        }

        @Bean
        StpLogic stpLogic() {
            StpLogic logic = mock(StpLogic.class);
            doThrow(new IllegalStateException("未登录")).when(logic).checkLogin();
            return logic;
        }

        @Bean
        RequestTraceInterceptor requestTraceInterceptor() throws Exception {
            RequestTraceInterceptor interceptor = mock(RequestTraceInterceptor.class);
            when(interceptor.preHandle(any(), any(), any())).thenReturn(true);
            return interceptor;
        }

        @Bean
        StockMonitorService stockMonitorService() {
            StockMonitorService service = mock(StockMonitorService.class);
            when(service.dashboard()).thenReturn(new StockMonitorDtos.Dashboard(1, false, null, List.of()));
            return service;
        }
    }

    @RestController
    static class PrivateProbeController {
        @GetMapping("/private/probe")
        CommonResult<String> probe() {
            return CommonResult.success("private");
        }
    }
}
