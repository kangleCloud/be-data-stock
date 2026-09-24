package com.vita.app.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.dto
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户注册请求
 * @Version: 1.0
 */
@Data
public class AppRegisterDto {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度必须在4到32位之间")
    private String userName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须在8到64位之间")
    private String password;

    @Size(max = 64, message = "昵称长度不能超过64位")
    private String nickName;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    @NotBlank(message = "验证码UUID不能为空")
    private String captchaUuid;

    @NotBlank(message = "验证码加密数据不能为空")
    private String captchaEncryptData;

    private String captchaMix;
}
