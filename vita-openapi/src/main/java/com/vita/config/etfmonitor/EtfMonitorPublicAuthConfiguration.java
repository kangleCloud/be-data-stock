package com.vita.config.etfmonitor;

import com.vita.auth.config.AuthExcludePathsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 仅 ETF 看板 GET 和 SSE 精确路径允许匿名。 */
@Configuration
public class EtfMonitorPublicAuthConfiguration {
    @Bean
    public AuthExcludePathsProvider etfMonitorPublicPaths() {
        return () -> List.of("/etf-monitor/v1/dashboard", "/etf-monitor/v1/stream");
    }
}
