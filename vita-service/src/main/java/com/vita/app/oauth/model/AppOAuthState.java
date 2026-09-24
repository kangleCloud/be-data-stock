package com.vita.app.oauth.model;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.model
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: Redis中的一次性OAuth状态
 * @Version: 1.0
 */
@Data
public class AppOAuthState {

    private String providerCode;

    private AppOAuthPurpose purpose;

    private Long userId;

    private String codeVerifier;
}
