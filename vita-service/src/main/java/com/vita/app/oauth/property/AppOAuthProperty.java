package com.vita.app.oauth.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.property
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App OAuth流程属性
 * @Version: 1.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vita.app.oauth")
public class AppOAuthProperty {

    /**
     * OAuth回调完成后唯一允许跳转的前端地址。
     */
    private String frontendRedirectUri;

    /**
     * OAuth state有效期。
     */
    private Duration stateTimeout = Duration.ofMinutes(5);

    /**
     * OAuth一次性登录票据有效期。
     */
    private Duration ticketTimeout = Duration.ofSeconds(60);
}
