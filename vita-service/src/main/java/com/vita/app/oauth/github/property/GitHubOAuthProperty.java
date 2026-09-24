package com.vita.app.oauth.github.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.github.property
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: GitHub OAuth平台属性
 * @Version: 1.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vita.app.oauth.github")
public class GitHubOAuthProperty {

    private boolean enabled;

    private String clientId;

    private String clientSecret;

    private String callbackUri;

    private String authorizationUri = "https://github.com/login/oauth/authorize";

    private String tokenUri = "https://github.com/login/oauth/access_token";

    private String userUri = "https://api.github.com/user";

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(5);
}
