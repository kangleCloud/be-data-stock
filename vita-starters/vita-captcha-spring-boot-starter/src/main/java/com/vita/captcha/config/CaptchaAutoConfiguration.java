package com.vita.captcha.config;

import com.google.code.kaptcha.Producer;
import com.vita.captcha.common.enums.CaptchaTypeEnum;
import com.vita.captcha.config.property.CaptchaProperty;
import com.vita.captcha.service.CaptchaApplicationService;
import com.vita.captcha.service.GraphCaptchaServiceImpl;
import com.vita.captcha.service.ICaptchaService;
import com.vita.captcha.service.SlideCaptchaServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.config
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码自动装配
 */
@Configuration
@EnableConfigurationProperties(value = {CaptchaProperty.class})
@ConditionalOnProperty(prefix = "vita.captcha", name = "enabled", havingValue = "true")
public class CaptchaAutoConfiguration {
    /**
     * 验证码应用服务 bean
     */
    @Bean
    @ConditionalOnMissingBean(CaptchaApplicationService.class)
    public CaptchaApplicationService captchaApplicationService(CaptchaProperty captchaProperty,
                                                               Map<String, ICaptchaService> captchaServiceMap) {
        return new CaptchaApplicationService(captchaProperty, captchaServiceMap);
    }

    /**
     * 滑动验证码bean
     */
    @Bean
    @ConditionalOnMissingBean(SlideCaptchaServiceImpl.class)
    @Conditional(CaptchaTypeCondition.SlideTypeCondition.class)
    public SlideCaptchaServiceImpl slideCaptchaService(CaptchaProperty captchaProperty) {
        return new SlideCaptchaServiceImpl(captchaProperty);
    }

    /**
     * 普通图形验证码bean
     */
    @Bean
    @ConditionalOnMissingBean({GraphCaptchaServiceImpl.class})
    @Conditional(CaptchaTypeCondition.GraphTypeCondition.class)
    public GraphCaptchaServiceImpl graphCaptchaService(CaptchaProperty captchaProperty,
                                                       @Qualifier("captchaProducer") Producer captchaProducer,
                                                       @Qualifier("captchaProducerMath") Producer captchaProducerMath) {
        return new GraphCaptchaServiceImpl(captchaProperty, captchaProducer, captchaProducerMath);
    }

}
