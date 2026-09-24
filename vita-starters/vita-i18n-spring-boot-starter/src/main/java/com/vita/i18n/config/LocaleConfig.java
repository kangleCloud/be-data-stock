package com.vita.i18n.config;

import com.vita.core.exception.ExceptionMessageResolver;
import com.vita.i18n.handler.MessageSourceHandler;
import com.vita.i18n.handler.I18nExceptionMessageResolver;
import com.vita.i18n.property.I18nProperty;
import com.vita.i18n.resolver.CustomLocaleResolver;
import jakarta.servlet.Servlet;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.i18n.config
 * @Author: znk
 * @CreateTime: 2026-03-20  23:47:58
 * @Description: 国际化配置
 * @Version: 1.0
 */
@AutoConfiguration(before = {MessageSourceAutoConfiguration.class, WebMvcAutoConfiguration.class})
@EnableConfigurationProperties(I18nProperty.class)
@ConditionalOnClass({Servlet.class, LocaleResolver.class, MessageSource.class})
@ConditionalOnProperty(prefix = "vita.i18n", name = "enabled", havingValue = "true")
public class LocaleConfig {

    @Bean
    @ConditionalOnMissingBean(LocaleResolver.class)
    public LocaleResolver localeResolver(I18nProperty i18nProperty) {
        i18nProperty.normalizeAndValidate();
        return new CustomLocaleResolver(i18nProperty.resolveDefaultLocale());
    }

    /**
     * 国际化资源配置
     *
     * @return 国际化资源配置
     */
    @Bean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
    @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
    public MessageSource messageSource(I18nProperty i18nProperty) {
        i18nProperty.normalizeAndValidate();
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/message");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    /**
     * 国际化消息处理器
     *
     * @return 国际化消息处理器
     */
    @Bean
    @ConditionalOnMissingBean(MessageSourceHandler.class)
    public MessageSourceHandler messageSourceHandler(MessageSource messageSource, I18nProperty i18nProperty) {
        i18nProperty.normalizeAndValidate();
        return new MessageSourceHandler(messageSource, i18nProperty);
    }

    @Bean
    @ConditionalOnMissingBean(ExceptionMessageResolver.class)
    public ExceptionMessageResolver exceptionMessageResolver(MessageSourceHandler messageSourceHandler) {
        return new I18nExceptionMessageResolver(messageSourceHandler);
    }
}
