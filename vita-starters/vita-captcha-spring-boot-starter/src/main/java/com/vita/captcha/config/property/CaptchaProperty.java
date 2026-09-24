package com.vita.captcha.config.property;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.captcha.common.enums.CaptchaTypeEnum;
import com.vita.core.exception.ServiceException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.config.property
 * @Author: znk
 * @CreateTime: 2026-03-20  21:28:55
 * @Description: 验证码属性类
 * @Version: 1.0
 */
@ConfigurationProperties(prefix = "vita.captcha")
@Data
public class CaptchaProperty {
    /**
     * 是否启用验证码。
     */
    private boolean enabled = true;
    /**
     * 验证码类型 char=图形文本验证码 math=图形答案验证码 slide=拼图滑动验证码 all_slide=全滑动验证码
     */
    private String captchaType = CaptchaTypeEnum.ALL_SLIDE.getType();
    /**
     * AES秘钥
     */
    private String aesSecret = "vita123456789012";
    /**
     * AES的iv值
     */
    private String aesIv = "vita123456789012";
    /**
     * 有效期，单位分钟。
     */
    private Integer expired = 3;
    /**
     * 滑动属性
     */
    private SlideCaptchaProperty slide = new SlideCaptchaProperty();

    /**
     * 规范化并校验属性。
     */
    public void normalizeAndValidate() {
        this.captchaType = CaptchaTypeEnum.normalize(this.captchaType);
        if (!enabled) {
            return;
        }
        CaptchaTypeEnum.fromType(this.captchaType);
        validateAesValue(aesSecret, "aesSecret");
        validateAesValue(aesIv, "aesIv");
        if (expired == null || expired <= 0) {
            throw new ServiceException("验证码有效期必须大于 0");
        }
        if (slide == null) {
            slide = new SlideCaptchaProperty();
        }
        slide.applyDefaults();
    }

    private void validateAesValue(String value, String fieldName) {
        if (CharSequenceUtil.isBlank(value) || value.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new ServiceException(fieldName + " 必须为 16 位字符串");
        }
    }
}
