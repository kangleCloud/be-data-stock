package com.vita.config;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.auth.config.SaTokenConfigure;
import com.vita.auth.property.AuthProperty;
import com.vita.controller.market.MarketDashboardController;
import com.vita.core.CommonResult;
import com.vita.core.exception.ControllerExceptionHandler;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.market.service.MarketSnapshotService;
import com.vita.market.service.MarketSnapshotStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MarketDashboardOpenApiAuthTest {

    @Test
    void openApiConfigurationExemptsOnlyExactDashboardPaths() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        var properties = yaml.getObject();
        assertThat(properties.getProperty("vita.auth.extra-exclude-paths[0]"))
                .isEqualTo("/market/dashboard/snapshot");
        assertThat(properties.getProperty("vita.auth.extra-exclude-paths[1]"))
                .isEqualTo("/market/dashboard/stream");
        assertThat(properties.getProperty("vita.auth.extra-exclude-paths[2]")).isNull();
    }

    @Test
    void onlyTwoExactDashboardPathsAreAnonymous() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestConfiguration.class);
            context.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            SseEmitter emitter = new SseEmitter(60_000L);
            when(context.getBean(MarketSnapshotStreamService.class).open()).thenReturn(emitter);

            mvc.perform(get("/openapi/api/market/dashboard/snapshot").contextPath("/openapi/api"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.schemaVersion").value(1));
            mvc.perform(get("/openapi/api/market/dashboard/stream").contextPath("/openapi/api"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted());
            emitter.complete();

            mvc.perform(get("/openapi/api/private/probe").contextPath("/openapi/api"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
            verify(context.getBean(StpLogic.class)).checkLogin();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @org.springframework.context.annotation.Import({SaTokenConfigure.class,
            ControllerExceptionHandler.class, MarketDashboardController.class, PrivateProbeController.class})
    static class TestConfiguration {

        @Bean
        AuthProperty authProperty() {
            AuthProperty property = new AuthProperty();
            property.setExtraExcludePaths(List.of(
                    "/market/dashboard/snapshot", "/market/dashboard/stream"));
            return property;
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
        MarketSnapshotService marketSnapshotService() throws Exception {
            MarketSnapshotService service = mock(MarketSnapshotService.class);
            when(service.getSnapshot()).thenReturn(new ObjectMapper().readTree(
                    "{\"schemaVersion\":1,\"modules\":{}}"));
            return service;
        }

        @Bean
        MarketSnapshotStreamService marketSnapshotStreamService() {
            return mock(MarketSnapshotStreamService.class);
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
