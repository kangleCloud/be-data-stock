package com.vita.auth.vo;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.vo
 * @Author: Codex
 * @CreateTime: 2026-03-21
 * @Description: 登录响应
 * @Version: 1.0
 */
@Data
public class AuthLoginVo {

    /**
     * token 名称（通常为 Authorization）
     */
    private String tokenName;

    /**
     * token 值
     */
    private String tokenValue;

    /**
     * token 前缀（通常为 Bearer）
     */
    private String tokenPrefix;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 当前登录用户ID
     */
    private Long userId;

    /**
     * 当前登录用户名称
     */
    private String username;

    /**
     * 当前登录用户昵称
     */
    private String nickName;


}
