package com.vita.workflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.config
 * @Author: znk
 * @CreateTime: 2026-07-14
 * @Description: 工作流启用条件测试
 * @Version: 1.0
 */
class ConditionalOnWorkflowEnabledTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(WorkflowProbeConfiguration.class);

    /**
     * 未配置开关时默认启用工作流 Bean。
     */
    @Test
    void workflowShouldBeEnabledByDefault() {
        contextRunner.run(context -> assertThat(context).hasBean("workflowProbe"));
    }

    /**
     * 显式关闭开关时不注册工作流 Bean。
     */
    @Test
    void workflowShouldBeDisabledByProperty() {
        contextRunner.withPropertyValues("vita.workflow.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("workflowProbe"));
    }

    @Configuration(proxyBeanMethods = false)
    static class WorkflowProbeConfiguration {

        @Bean
        @ConditionalOnWorkflowEnabled
        String workflowProbe() {
            return "enabled";
        }
    }
}
