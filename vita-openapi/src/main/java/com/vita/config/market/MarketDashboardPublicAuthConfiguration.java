package com.vita.config.market;

import com.vita.auth.config.AuthExcludePathsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 仅公开市场快照 GET 和 SSE 的精确路径。 */
@Configuration
public class MarketDashboardPublicAuthConfiguration {
    @Bean
    public AuthExcludePathsProvider marketDashboardPublicPaths() {
        return () -> List.of("/market/dashboard/snapshot", "/market/dashboard/stream");
    }
}
