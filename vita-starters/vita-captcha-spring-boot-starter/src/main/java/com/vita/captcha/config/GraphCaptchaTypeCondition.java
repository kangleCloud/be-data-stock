package com.vita.captcha.config;

import com.vita.captcha.common.enums.CaptchaTypeEnum;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** 图形验证码生效条件。 */
public class GraphCaptchaTypeCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return CaptchaTypeEnum.isGraphType(CaptchaTypeCondition.getCaptchaType(context));
    }
}
