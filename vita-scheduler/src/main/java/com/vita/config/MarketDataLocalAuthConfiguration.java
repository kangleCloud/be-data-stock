package com.vita.config;

import com.vita.auth.config.AuthExcludePathsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 精确放行八个入口，真实回环与转发头检查由 Controller 执行。 */
@Configuration
public class MarketDataLocalAuthConfiguration {
    @Bean
    public AuthExcludePathsProvider marketDataLocalPaths() {
        return () -> List.of(
                "/local/market-data/v1/stock/dictionary/refresh",
                "/local/market-data/v1/stock/profiles/refresh",
                "/local/market-data/v1/calendar/refresh",
                "/local/market-data/v1/market/refresh",
                "/local/market-data/v1/stock/quotes/refresh",
                "/local/market-data/v1/etf/dictionary/refresh",
                "/local/market-data/v1/etf/profiles/refresh",
                "/local/market-data/v1/etf/quotes/refresh");
    }
}
