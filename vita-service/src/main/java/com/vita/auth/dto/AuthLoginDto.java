package com.vita.auth.dto;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.dto
 * @Author: Codex
 * @CreateTime: 2026-03-21
 * @Description: 管理端登录请求
 * @Version: 1.0
 */
@Data
public class AuthLoginDto {

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 验证码 code
     */
    private String captchaCode;

    /**
     * 验证码 UUID
     */
    private String captchaUuid;

    /**
     * 验证码加密数据
     */
    private String captchaEncryptData;

    /**
     * 验证码混合
     */
    private String captchaMix;
}
