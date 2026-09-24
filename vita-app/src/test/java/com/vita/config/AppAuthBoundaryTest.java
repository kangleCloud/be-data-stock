package com.vita.config;

import com.vita.controller.auth.AppAuthController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App认证模块与安全边界测试
 * @Version: 1.0
 */
class AppAuthBoundaryTest {

    @Test
    void appShouldBeIndependentModuleWithoutGeneratedAdminCrud() throws IOException {
        Path repository = repositoryRoot();
        assertThat(Files.readString(repository.resolve("pom.xml"))).contains("<module>vita-app</module>");
        assertThat(repository.resolve("vita-admin/src/main/java/com/vita/controller/app/AppUserController.java"))
                .doesNotExist();
        assertThat(repository.resolve("vita-admin/src/main/java/com/vita/controller/app/AppOauthAccountController.java"))
                .doesNotExist();

        String generator = Files.readString(repository.resolve(
                "vita-generator/src/main/java/com/vita/config/CodeGenerator.java"));
        assertThat(generator).contains(
                "private static final String[] TABLE_NAMES = {\"workflow_category\"};",
                "Set.of(\"app_user\", \"app_oauth_account\")"
        );

        Path appSource = repository.resolve("vita-app/src/main/java");
        try (var files = Files.walk(appSource)) {
            List<String> javaFiles = files.filter(path -> path.toString().endsWith(".java"))
                    .map(appSource::relativize)
                    .map(Path::toString)
                    .sorted()
                    .toList();
            assertThat(javaFiles).containsExactly(
                    "com/vita/config/VitaAppApplication.java",
                    "com/vita/controller/auth/AppAuthController.java"
            );
        }
    }

    @Test
    void anonymousRoutesShouldNotIncludeBindingOrCurrentUser() throws IOException {
        String yaml = Files.readString(repositoryRoot().resolve("vita-app/src/main/resources/application.yml"));

        assertThat(yaml).contains(
                "login-type: app",
                "- /auth/register",
                "- /auth/login",
                "- /auth/oauth/*/authorize",
                "- /auth/oauth/*/callback",
                "- /auth/oauth/ticket/exchange",
                "- /captcha/**"
        );
        assertThat(yaml).doesNotContain("- /auth/me", "- /auth/logout", "- /auth/oauth/*/bind-authorize");
    }

    @Test
    void appProfilesShouldUseRepositoryConfigurationValues() throws IOException {
        Path resources = repositoryRoot().resolve("vita-app/src/main/resources");
        String devYaml = Files.readString(resources.resolve("application-dev.yml"));
        String prodYaml = Files.readString(resources.resolve("application-prod.yml"));

        assertThat(devYaml).doesNotContain("${VITA_");
        assertThat(prodYaml).doesNotContain("${VITA_");
        assertThat(devYaml).contains("enabled: false", "client-id:", "client-secret:", "callback-uri:");
        assertThat(prodYaml).contains("enabled: false", "client-id:", "client-secret:", "callback-uri:");
    }

    @Test
    void controllerShouldUseOnlyGetAndPostWithExplicitParameterNames() {
        for (Method method : AppAuthController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PutMapping.class)).isNull();
            assertThat(method.getAnnotation(DeleteMapping.class)).isNull();
            assertThat(method.getAnnotation(PatchMapping.class)).isNull();
            for (Parameter parameter : method.getParameters()) {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                if (requestParam != null) {
                    assertThat(requestParam.name()).isNotBlank();
                }
            }
        }
    }

    @Test
    void passwordAndOauthSecretsShouldStayInsideControlledBoundaries() throws IOException {
        Path repository = repositoryRoot();
        String initSql = Files.readString(repository.resolve("sql/init/app.sql"));
        String upgradeSql = Files.readString(repository.resolve(
                "sql/upgrade/20260825_vita_app_auth_upgrade.sql"));
        String userService = Files.readString(repository.resolve(
                "vita-service/src/main/java/com/vita/app/user/service/impl/AppUserServiceImpl.java"));
        String githubProvider = Files.readString(repository.resolve(
                "vita-service/src/main/java/com/vita/app/oauth/github/GitHubOAuthProvider.java"));

        assertThat(initSql).contains("`password`").doesNotContain("password_hash", "access_token", "refresh_token");
        assertThat(upgradeSql).contains(
                "COLUMN_NAME = 'password_hash'",
                "CHANGE COLUMN `password_hash` `password`"
        );
        assertThat(userService).contains("Sm4Utils.encryptToBase64").doesNotContain("AppPasswordCodec");
        assertThat(githubProvider).doesNotContain("setAccessToken", "setRefreshToken");
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("vita-app"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录");
    }
}
