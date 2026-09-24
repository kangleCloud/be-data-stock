package com.vita.core.exception;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.exception
 * @Author: znk
 * @CreateTime: 2026-03-21  01:05:00
 * @Description: 统一异常消息解析扩展点
 * @Version: 1.0
 */
public interface ExceptionMessageResolver {

    /**
     * 解析标准错误码消息。
     *
     * @param errorCode 错误码
     * @return 解析后的消息
     */
    String resolveErrorCode(ErrorCode errorCode);

    /**
     * 解析业务异常消息。
     *
     * @param code    业务错误码
     * @param message 原始异常消息
     * @return 解析后的消息
     */
    String resolveServiceMessage(Integer code, String message);

    /**
     * 解析参数校验异常消息。
     *
     * @param message 原始校验消息
     * @return 解析后的消息
     */
    String resolveValidationMessage(String message);
}
