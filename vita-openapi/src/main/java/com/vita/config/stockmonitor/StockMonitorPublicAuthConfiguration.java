package com.vita.config.stockmonitor;

import com.vita.auth.config.AuthExcludePathsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 仅个股监控大屏 GET 路径匿名可读。 */
@Configuration
public class StockMonitorPublicAuthConfiguration {
    @Bean
    public AuthExcludePathsProvider stockMonitorPublicPaths() {
        return () -> List.of("/stock-monitor/v1/dashboard");
    }
}
