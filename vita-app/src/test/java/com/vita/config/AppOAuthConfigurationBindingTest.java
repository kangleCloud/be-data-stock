package com.vita.config;

import com.vita.app.oauth.github.property.GitHubOAuthProperty;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-08-31
 * @Description: App OAuth配置绑定测试
 * @Version: 1.0
 */
class AppOAuthConfigurationBindingTest {

    @Test
    void disabledGithubShouldAllowEmptyConfigurationInAllProfiles() throws IOException {
        assertDisabledGithubConfiguration("application-dev.yml");
        assertDisabledGithubConfiguration("application-prod.yml");
    }

    private void assertDisabledGithubConfiguration(String resourceName) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load(resourceName, new ClassPathResource(resourceName))
                .forEach(environment.getPropertySources()::addFirst);

        GitHubOAuthProperty property = Binder.get(environment)
                .bind("vita.app.oauth.github", Bindable.of(GitHubOAuthProperty.class))
                .orElseThrow(() -> new IllegalStateException("无法绑定配置：" + resourceName));
        assertThat(property.isEnabled()).isFalse();
        assertThat(property.getClientId()).isNullOrEmpty();
        assertThat(property.getClientSecret()).isNullOrEmpty();
        assertThat(property.getCallbackUri()).isNullOrEmpty();
    }
}
