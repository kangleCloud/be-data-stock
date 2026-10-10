package com.vita.controller.stream;

import cn.dev33.satoken.spring.SaTokenContextRegister;
import cn.dev33.satoken.stp.StpLogic;
import com.vita.auth.config.SaTokenConfigure;
import com.vita.auth.handler.SaTokenExceptionHandler;
import com.vita.auth.property.AuthProperty;
import com.vita.config.etfmonitor.EtfMonitorPublicAuthConfiguration;
import com.vita.config.market.MarketDashboardPublicAuthConfiguration;
import com.vita.config.stockmonitor.StockMonitorPublicAuthConfiguration;
import com.vita.controller.etfmonitor.EtfMonitorDashboardController;
import com.vita.controller.market.MarketDashboardController;
import com.vita.controller.stockmonitor.StockMonitorDashboardController;
import com.vita.core.exception.ControllerExceptionHandler;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.marketdata.etfmonitor.service.EtfMonitorDashboardService;
import com.vita.marketdata.etfmonitor.service.EtfMonitorStreamService;
import com.vita.marketdata.market.service.MarketSnapshotService;
import com.vita.marketdata.market.service.MarketSnapshotStreamService;
import com.vita.marketdata.stockmonitor.service.StockMonitorService;
import com.vita.marketdata.stockmonitor.service.StockMonitorStreamService;
import com.vita.web.xss.XssConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

/** 使用真实 HTTP 容器和生产 MVC／鉴权配置，隔离数据库、Redis 与本机私有 YAML。 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class, TaskExecutionAutoConfiguration.class, SaTokenContextRegister.class})
@Import({MarketDashboardController.class, StockMonitorDashboardController.class, EtfMonitorDashboardController.class,
        ControllerExceptionHandler.class, SaTokenExceptionHandler.class, SaTokenConfigure.class, AuthProperty.class,
        MarketDashboardPublicAuthConfiguration.class, StockMonitorPublicAuthConfiguration.class,
        EtfMonitorPublicAuthConfiguration.class, SsePrivateTestController.class, XssConfig.class})
public class SseHttpTestConfiguration {
    @Bean
    MarketSnapshotService marketSnapshots() {
        return mock(MarketSnapshotService.class);
    }

    @Bean
    MarketSnapshotStreamService marketStream() {
        return mock(MarketSnapshotStreamService.class);
    }

    @Bean
    StockMonitorService stockDashboard() {
        return mock(StockMonitorService.class);
    }

    @Bean
    StockMonitorStreamService stockStream() {
        return mock(StockMonitorStreamService.class);
    }

    @Bean
    EtfMonitorDashboardService etfDashboard() {
        return mock(EtfMonitorDashboardService.class);
    }

    @Bean
    EtfMonitorStreamService etfStream() {
        return mock(EtfMonitorStreamService.class);
    }

    @Bean
    StpLogic loginLogic() {
        return mock(StpLogic.class);
    }

    @Bean
    RequestTraceInterceptor requestTraceInterceptor() {
        return mock(RequestTraceInterceptor.class);
    }
}
