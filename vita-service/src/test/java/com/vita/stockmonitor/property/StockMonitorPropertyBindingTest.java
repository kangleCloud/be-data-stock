package com.vita.stockmonitor.property;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockMonitorPropertyBindingTest {

    @Test
    void devTemplateUsesLocalPythonAndDisablesXueqiuByDefault() throws IOException {
        StockMonitorProperty property = bindTemplate("dev", null);

        assertThat(property.getPythonBaseUrl()).isEqualTo("http://127.0.0.1:8000");
        assertThat(property.getInternalToken()).isEmpty();
        assertThat(property.isXqEnabled()).isFalse();
    }

    @Test
    void prodTemplateNeedsDeploymentValuesAndBindsEnvironmentOverrides() throws IOException {
        StockMonitorProperty defaults = bindTemplate("prod", null);
        assertThat(defaults.getPythonBaseUrl()).isEmpty();
        assertThat(defaults.getInternalToken()).isEmpty();
        assertThat(defaults.isXqEnabled()).isFalse();

        MockEnvironment deployment = new MockEnvironment()
                .withProperty("STOCK_MONITOR_PYTHON_BASE_URL", "http://python.internal:8000")
                .withProperty("STOCK_MONITOR_INTERNAL_TOKEN", "test-token")
                .withProperty("STOCK_MONITOR_XQ_ENABLED", "true");
        StockMonitorProperty overridden = bindTemplate("prod", deployment);
        assertThat(overridden.getPythonBaseUrl()).isEqualTo("http://python.internal:8000");
        assertThat(overridden.getInternalToken()).isEqualTo("test-token");
        assertThat(overridden.isXqEnabled()).isTrue();
    }

    @Test
    void localProfilesWhenPresentMustMatchSafeTrackedTemplates() throws IOException {
        Path repository = findTemplate("prod").getParent().getParent();
        int present = 0;
        for (String module : List.of("vita-admin", "vita-scheduler", "vita-openapi")) {
            for (String profile : List.of("dev", "prod")) {
                Path local = repository.resolve(module + "/src/main/resources/application-" + profile + ".yml");
                if (!Files.isRegularFile(local)) {
                    continue; // application*.yml 被 Git 忽略，干净检出仅验证上面的可跟踪模板。
                }
                present++;
                PropertySource<?> actual = loadYaml(local);
                PropertySource<?> template = loadYaml(findTemplate(profile));
                for (String key : List.of("python-base-url", "internal-token", "xq-enabled")) {
                    String name = "vita.stock-monitor." + key;
                    // 比较布尔值，避免断言失败时输出本机令牌。
                    assertThat(template.getProperty(name).equals(actual.getProperty(name)))
                            .as(module + " " + profile + " " + name).isTrue();
                }
            }
        }
        assertThat(present).as("本机 profile 文件须全部存在或全部由部署环境提供").isIn(0, 6);
    }

    private StockMonitorProperty bindTemplate(String profile, MockEnvironment suppliedEnvironment) throws IOException {
        Path template = findTemplate(profile);
        MockEnvironment environment = suppliedEnvironment == null ? new MockEnvironment() : suppliedEnvironment;
        environment.getPropertySources().addLast(loadYaml(template));
        return Binder.get(environment)
                .bind("vita.stock-monitor", Bindable.of(StockMonitorProperty.class))
                .orElseThrow(() -> new IllegalStateException("Unable to bind " + template));
    }

    private PropertySource<?> loadYaml(Path file) throws IOException {
        return new YamlPropertySourceLoader().load(file.getFileName().toString(), new FileSystemResource(file)).get(0);
    }

    private Path findTemplate(String profile) {
        for (Path directory = Path.of("").toAbsolutePath(); directory != null; directory = directory.getParent()) {
            Path template = directory.resolve("config/stock-monitor-" + profile + ".example.yml");
            if (Files.isRegularFile(template)) {
                return template;
            }
        }
        throw new IllegalStateException("Stock monitor configuration template not found: " + profile);
    }
}
