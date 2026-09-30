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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StockMonitorPropertyBindingTest {

    private static final Pattern ENVIRONMENT_PLACEHOLDER = Pattern.compile("\\$\\{[A-Z][A-Z0-9_]*(?::[^}]*)?}");

    @Test
    void devTemplateUsesLocalPythonAndDisablesXueqiuByDefault() throws IOException {
        StockMonitorProperty property = bindTemplate("dev", null);

        assertThat(property.getPythonBaseUrl()).isEqualTo("http://127.0.0.1:8000");
        assertThat(property.getInternalToken()).isEmpty();
        assertThat(property.isXqEnabled()).isFalse();
    }

    @Test
    void prodTemplateNeedsLocalValuesAndIgnoresUnmappedEnvironmentVariables() throws IOException {
        StockMonitorProperty defaults = bindTemplate("prod", null);
        assertThat(defaults.getPythonBaseUrl()).isEmpty();
        assertThat(defaults.getInternalToken()).isEmpty();
        assertThat(defaults.isXqEnabled()).isFalse();

        MockEnvironment deployment = new MockEnvironment()
                .withProperty("STOCK_MONITOR_PYTHON_BASE_URL", "http://python.internal:8000")
                .withProperty("STOCK_MONITOR_INTERNAL_TOKEN", "test-token")
                .withProperty("STOCK_MONITOR_XQ_ENABLED", "true");
        StockMonitorProperty unchanged = bindTemplate("prod", deployment);
        assertThat(unchanged.getPythonBaseUrl()).isEmpty();
        assertThat(unchanged.getInternalToken()).isEmpty();
        assertThat(unchanged.isXqEnabled()).isFalse();
    }

    @Test
    void trackedTemplatesLeaveSecretsBlankAndDefaultXueqiuOff() throws IOException {
        for (String profile : new String[]{"dev", "prod"}) {
            PropertySource<?> template = loadYaml(findTemplate(profile));
            assertThat(template.getProperty("vita.stock-monitor.internal-token"))
                    .isEqualTo("");
            assertThat(template.getProperty("vita.stock-monitor.xq-enabled"))
                    .isEqualTo(false);
        }
        assertThat(loadYaml(findTemplate("prod")).getProperty("vita.stock-monitor.python-base-url"))
                .isEqualTo("");
    }

    @Test
    void applicationProfilesDoNotInjectEnvironmentVariables() throws IOException {
        Path repository = findTemplate("prod").getParent().getParent();
        try (Stream<Path> files = Files.walk(repository)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("application-.*\\.yml"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .toList()) {
                assertThat(ENVIRONMENT_PLACEHOLDER.matcher(Files.readString(file)).find())
                        .as("环境变量占位符：" + repository.relativize(file)).isFalse();
            }
        }
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
