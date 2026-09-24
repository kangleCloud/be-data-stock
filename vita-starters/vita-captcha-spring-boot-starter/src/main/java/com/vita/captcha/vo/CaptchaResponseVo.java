package com.vita.captcha.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.vo
 * @Author: znk
 * @CreateTime: 2026-03-20  21:40:04
 * @Description: 验证码响应 VO，包含返回给前端的验证码相关信息,继承自 CaptchaBasicResponseVo
 * @Version: 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CaptchaResponseVo extends CaptchaBasicResponseVo {

    /**
     * 随机字符串
     **/
    private String uuid;
    /**
     * 验证码类型 char=图形文本验证码 math=图形答案验证码 slide=拼图滑动验证码 all_slide=全滑动验证码
     */
    private String captchaType;

    /**
     * 加密数据
     */
    private String encryptData;
    /**
     * 验证码扩展数据，滑动验证码时返回滑动元数据。
     */
    private Object captchaData;
}
