package com.vita.app.auth.vo;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.vo
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App登录令牌响应
 * @Version: 1.0
 */
@Data
public class AppAuthTokenVo {

    private String tokenName;

    private String tokenValue;

    private String tokenPrefix;

    private Long expiresIn;

    private AppUserProfileVo user;
}
