package com.vita.i18n.handler;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.ErrorCode;
import com.vita.core.exception.ExceptionMessageResolver;
import com.vita.i18n.constant.I18nConstant;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.i18n.handler
 * @Author: znk
 * @CreateTime: 2026-03-20  23:44:46
 * @Description: 国际化异常消息解析器
 * @Version: 1.0
 */
public class I18nExceptionMessageResolver implements ExceptionMessageResolver {
    private final MessageSourceHandler messageSourceHandler;

    public I18nExceptionMessageResolver(MessageSourceHandler messageSourceHandler) {
        this.messageSourceHandler = messageSourceHandler;
    }

    @Override
    public String resolveErrorCode(ErrorCode errorCode) {
        return messageSourceHandler.getMessage(errorCode);
    }

    @Override
    public String resolveServiceMessage(Integer code, String message) {
        String resolvedMessage = messageSourceHandler.getMessageOrDefault(message, message);
        if (!CharSequenceUtil.equals(resolvedMessage, message)) {
            return resolvedMessage;
        }
        if (code != null && (CharSequenceUtil.isBlank(message) || I18nConstant.isDefaultErrorMessage(code, message))) {
            return messageSourceHandler.getMessageOrDefault(I18nConstant.buildErrorCodeKey(code), message);
        }
        return message;
    }

    @Override
    public String resolveValidationMessage(String message) {
        return messageSourceHandler.getMessageOrDefault(message, message);
    }
}
