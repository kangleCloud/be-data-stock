package com.vita.i18n.handler;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.ErrorCode;
import com.vita.i18n.constant.I18nConstant;
import com.vita.i18n.property.I18nProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.i18n.handler
 * @Author: znk
 * @CreateTime: 2026-03-20  23:40:33
 * @Description: 国际化消息处理
 * @Version: 1.0
 */
public class MessageSourceHandler {
    private final MessageSource messageSource;
    private final I18nProperty i18nProperty;

    public MessageSourceHandler(MessageSource messageSource, I18nProperty i18nProperty) {
        this.messageSource = messageSource;
        this.i18nProperty = i18nProperty;
    }

    /**
     * 获取消息
     */
    public String getMessage(ErrorCode errorCode) {
        return getMessage(I18nConstant.buildErrorCodeKey(errorCode.getCode()), null, errorCode.getMsg());
    }

    /**
     * 获取消息
     */
    public String getMessage(String code) {
        return getMessage(code, null, code);
    }

    /**
     * 获取消息
     */
    public String getMessage(String code, Object[] params) {
        return getMessage(code, params, code);
    }

    /**
     * 获取消息，缺失时返回默认值。
     */
    public String getMessageOrDefault(String code, String defaultMessage) {
        return getMessage(code, null, defaultMessage);
    }

    public String getMessage(String code, Object[] params, String defaultMessage) {
        if (CharSequenceUtil.isBlank(code)) {
            return defaultMessage;
        }
        return messageSource.getMessage(code, params, defaultMessage, getLocale());
    }

    private Locale getLocale() {
        LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
        Locale locale = localeContext == null ? null : localeContext.getLocale();
        Locale resolvedLocale = locale == null ? null : I18nConstant.resolveLocale(locale.toLanguageTag());
        return resolvedLocale == null ? i18nProperty.resolveDefaultLocale() : resolvedLocale;
    }
}
