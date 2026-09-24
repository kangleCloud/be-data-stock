package com.vita.captcha.config;

import com.vita.captcha.common.enums.CaptchaTypeEnum;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 验证码类型条件判断。
 *
 * @author znk
 */
public final class CaptchaTypeCondition {

    private static final String CAPTCHA_TYPE_PROPERTY = "vita.captcha.captcha-type";

    private CaptchaTypeCondition() {
    }

    private static String getCaptchaType(ConditionContext context) {
        return CaptchaTypeEnum.normalize(context.getEnvironment().getProperty(CAPTCHA_TYPE_PROPERTY));
    }

    /**
     * 图形验证码条件。
     */
    public static class GraphTypeCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return CaptchaTypeEnum.isGraphType(getCaptchaType(context));
        }
    }

    /**
     * 滑动验证码条件。
     */
    public static class SlideTypeCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return CaptchaTypeEnum.isSlideType(getCaptchaType(context));
        }
    }
}
