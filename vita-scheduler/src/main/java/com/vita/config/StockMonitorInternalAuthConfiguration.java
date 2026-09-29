package com.vita.config;

import com.vita.auth.config.AuthExcludePathsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 整体刷新使用内部令牌；两个本机手动入口由控制器严格校验直连地址。 */
@Configuration
public class StockMonitorInternalAuthConfiguration {
    @Bean
    public AuthExcludePathsProvider stockMonitorInternalPath() {
        return () -> List.of("/internal/stock-monitor/v1/refresh",
                "/local/stock-monitor/v1/dictionary/refresh",
                "/local/stock-monitor/v1/profiles/refresh");
    }
}
