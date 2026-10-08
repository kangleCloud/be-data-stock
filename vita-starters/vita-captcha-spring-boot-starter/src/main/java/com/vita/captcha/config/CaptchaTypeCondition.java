package com.vita.captcha.config;

import com.vita.captcha.common.enums.CaptchaTypeEnum;
import org.springframework.context.annotation.ConditionContext;

/**
 * 验证码类型条件判断。
 *
 * @author znk
 */
public final class CaptchaTypeCondition {

    private static final String CAPTCHA_TYPE_PROPERTY = "vita.captcha.captcha-type";

    private CaptchaTypeCondition() {
    }

    static String getCaptchaType(ConditionContext context) {
        return CaptchaTypeEnum.normalize(context.getEnvironment().getProperty(CAPTCHA_TYPE_PROPERTY));
    }
}
