package com.vita.config;

import com.vita.auth.config.AuthExcludePathsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 仅三个本机手动任务 POST 路径免登录；控制器继续校验真实来源地址。 */
@Configuration
public class PythonJobsLocalAuthConfiguration {
    @Bean
    public AuthExcludePathsProvider pythonJobsLocalPaths() {
        return () -> List.of(
                "/local/python-jobs/v1/calendar/refresh",
                "/local/python-jobs/v1/market/refresh",
                "/local/python-jobs/v1/monitor/refresh",
                "/local/python-jobs/v1/etf/refresh");
    }
}
