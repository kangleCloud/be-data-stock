package com.vita.app.oauth.model;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.model
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: OAuth平台公开用户资料
 * @Version: 1.0
 */
@Data
public class AppOAuthUserProfile {

    private String providerCode;

    private String providerUserId;

    private String providerLogin;

    private String providerNickName;

    private String avatarUrl;

    private String profileUrl;
}
