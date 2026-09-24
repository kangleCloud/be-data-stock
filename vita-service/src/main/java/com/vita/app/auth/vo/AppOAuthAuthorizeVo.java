package com.vita.app.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.vo
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: OAuth授权地址响应
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
public class AppOAuthAuthorizeVo {

    private String authorizationUrl;
}
