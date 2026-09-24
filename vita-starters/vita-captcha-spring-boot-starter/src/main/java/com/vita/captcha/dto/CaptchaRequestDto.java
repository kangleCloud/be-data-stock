package com.vita.captcha.dto;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.dto
 * @Author: znk
 * @CreateTime: 2026-03-20  21:36:37
 * @Description: 验证码请求 DTO，包含生成验证码所需的参数
 * @Version: 1.0
 */
@Data
public class CaptchaRequestDto {
    /**
     * 混淆值，用户名登录时必须为用户名 手机登录时必须为手机号，其他情况可为空,为空时内部赋值为uuid
     */
    private String mix;
    /**
     * 滑动验证码请求参数 - 一般不用传使用默认
     */
    private SlideCaptchaRequestDto slideCaptcha;
    /**
     * uuid唯一值
     */
    private String uuid;
    /**
     * 生成的验证码值，内部赋值
     */
    private String captchaCode;
}
