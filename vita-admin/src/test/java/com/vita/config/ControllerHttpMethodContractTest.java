package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全仓 Controller HTTP 方法白名单契约测试。
 */
class ControllerHttpMethodContractTest {

    private static final Pattern FORBIDDEN_MAPPING = Pattern.compile(
            "@(?:Put|Delete|Patch)Mapping\\b"
                    + "|RequestMethod\\.(?!GET\\b|POST\\b)[A-Z]+\\b");

    @Test
    void controllersShouldOnlyDeclareGetAndPostMethods() throws IOException {
        Path repository = resolveRepositoryRoot();
        List<String> violations = new ArrayList<>();

        try (var modules = Files.list(repository)) {
            for (Path module : modules.filter(Files::isDirectory).toList()) {
                Path sourceRoot = module.resolve("src/main/java");
                if (!Files.isDirectory(sourceRoot)) {
                    continue;
                }
                try (var sources = Files.walk(sourceRoot)) {
                    for (Path source : sources.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                            .toList()) {
                        collectViolations(repository, source, violations);
                    }
                }
            }
        }

        assertThat(violations)
                .as("Controller 只能声明 GET 或 POST：%s", String.join(", ", violations))
                .isEmpty();
    }

    private void collectViolations(Path repository, Path source, List<String> violations)
            throws IOException {
        List<String> lines = Files.readAllLines(source);
        for (int index = 0; index < lines.size(); index++) {
            if (FORBIDDEN_MAPPING.matcher(lines.get(index)).find()) {
                violations.add(repository.relativize(source) + ":" + (index + 1));
            }
        }
    }

    private Path resolveRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("vita-admin"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录");
    }
}
