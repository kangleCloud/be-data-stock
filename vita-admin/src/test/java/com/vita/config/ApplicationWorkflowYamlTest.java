package com.vita.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-07-14
 * @Description: 工作流配置文件回归测试
 * @Version: 1.0
 */
class ApplicationWorkflowYamlTest {

    private static final List<String> EXPECTED_PUBLIC_PATHS = List.of(
            "/auth/login",
            "/captcha/**",
            "/public/**",
            "/warm-flow-ui/index.html",
            "/warm-flow-ui/css/**",
            "/warm-flow-ui/js/**",
            "/warm-flow-ui/ico/**",
            "/warm-flow-ui/config"
    );

    /**
     * 工作流白名单只能包含 UI 配置和静态资源。
     *
     * @throws IOException 配置文件读取异常
     */
    @Test
    void workflowWhitelistShouldOnlyExposeUiStaticResources() throws IOException {
        List<String> excludePaths = bindStringList("application.yml", "vita.auth.extra-exclude-paths");

        assertThat(excludePaths).containsExactlyElementsOf(EXPECTED_PUBLIC_PATHS);
        assertThat(excludePaths).doesNotContain("/warm-flow/**", "/warm-flow-ui/**");
        assertThat(bindStringList("application-dev.yml", "vita.auth.extra-exclude-paths")).isEmpty();
        assertThat(bindStringList("application-prod.yml", "vita.auth.extra-exclude-paths")).isEmpty();
    }

    /**
     * be-vita 工作流开关必须同时控制引擎和设计器。
     *
     * @throws IOException 配置文件读取异常
     */
    @Test
    void workflowSwitchShouldControlEngineAndUi() throws IOException {
        PropertySource<?> workflow = loadPropertySource("application-workflow.yml");

        assertThat(workflow.getProperty("vita.workflow.enabled"))
                .isEqualTo("${VITA_WORKFLOW_ENABLED:true}");
        assertThat(workflow.getProperty("warm-flow.enabled")).isEqualTo("${vita.workflow.enabled}");
        assertThat(workflow.getProperty("warm-flow.ui")).isEqualTo("${vita.workflow.enabled}");
        assertThat(workflow.getProperty("warm-flow.data-source-type")).isEqualTo("mysql");
        assertThat(workflow.getProperty("vita.auth.extra-exclude-paths[0]")).isNull();
    }

    /**
     * 工作流集成测试配置必须使用独立数据库和 Redis DB。
     *
     * @throws IOException 配置文件读取异常
     */
    @Test
    void workflowIntegrationProfileShouldBeIsolated() throws IOException {
        PropertySource<?> integration = loadPropertySource("application-workflow-integration.yml");

        assertThat(integration.getProperty("vita.workflow.enabled")).isEqualTo(true);
        assertThat(integration.getProperty("vita.mysql.master.database"))
                .isEqualTo("${VITA_WORKFLOW_IT_MYSQL_DATABASE:data_stock_workflow_it}");
        assertThat(integration.getProperty("spring.data.redis.database")).isEqualTo(14);
    }

    private List<String> bindStringList(String resourceName, String propertyName) throws IOException {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(loadPropertySource(resourceName));
        return new Binder(ConfigurationPropertySources.from(propertySources))
                .bind(propertyName, Bindable.listOf(String.class))
                .orElseGet(List::of);
    }

    private PropertySource<?> loadPropertySource(String resourceName) throws IOException {
        return new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName))
                .get(0);
    }
}
