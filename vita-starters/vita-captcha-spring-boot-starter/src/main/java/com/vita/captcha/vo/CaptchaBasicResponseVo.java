package com.vita.captcha.vo;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.vo
 * @Author: znk
 * @CreateTime: 2026-03-20  21:38:51
 * @Description: 验证码基础响应 VO，包含生成验证码后返回给前端的基本信息，如验证码图片的 Base64 编码、验证码唯一标识等
 * @Version: 1.0
 */
@Data
public class CaptchaBasicResponseVo {

    /**
     * 生成的验证码图片
     **/
    private String captchaImg;
}
