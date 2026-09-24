package com.vita.captcha.dto;

import lombok.Data;

/**
 * 验证码校验请求 DTO。
 *
 * @author znk
 */
@Data
public class CaptchaVerifyRequestDto {
    /**
     * 混淆值，未传时默认回退到 uuid。
     */
    private String mix;
    /**
     * 生成验证码时返回的 uuid。
     */
    private String uuid;
    /**
     * 用户提交的验证码值或滑动位移。
     */
    private String captchaCode;
    /**
     * 验证码加密数据。
     */
    private String encryptData;
}
