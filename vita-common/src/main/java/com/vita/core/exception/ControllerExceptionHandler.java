package com.vita.core.exception;

import com.vita.core.CommonResult;
import com.vita.core.CommonStreamResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.exception
 * @Author: znk
 * @CreateTime: 2026-03-04  21:18:47
 * @Description: 统一异常处理器
 * @Version: 1.0
 */
@ControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ControllerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);
    private final ObjectProvider<ExceptionMessageResolver> exceptionMessageResolverProvider;

    public ControllerExceptionHandler(ObjectProvider<ExceptionMessageResolver> exceptionMessageResolverProvider) {
        this.exceptionMessageResolverProvider = exceptionMessageResolverProvider;
    }

    /**
     * 处理参数绑定校验异常。
     *
     * @param e 异常
     * @return 返回结果
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public CommonResult validExceptionHandler(BindException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        String resolvedMessage = resolveValidationMessage(message);
        LOG.warn("参数校验失败：{}" , resolvedMessage);
        return CommonResult.error(resolvedMessage);
    }

    /**
     * JSON 请求体反序列化异常统一处理。
     *
     * @param e 异常
     * @return 返回结果
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    @ResponseBody
    public CommonResult validExceptionHandler(HttpMessageNotReadableException e) {
        String resolvedMessage = resolveValidationMessage("请求体不是合法的 JSON");
        LOG.warn("请求体解析失败：{}" , resolvedMessage);
        return CommonResult.error(GlobalErrorCode.BAD_REQUEST.getCode(), resolvedMessage);
    }

    /**
     * 不受支持的 HTTP 方法返回标准 405 状态和统一响应体。
     *
     * @param e 请求方法异常
     * @return 返回结果
     */
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public CommonResult methodNotAllowedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        LOG.warn("请求方法不受支持：{}" , e.getMethod());
        return CommonResult.error(
                GlobalErrorCode.METHOD_NOT_ALLOWED.getCode(),
                GlobalErrorCode.METHOD_NOT_ALLOWED.getMsg());
    }

    /**
     * 处理业务异常。
     *
     * @param e 异常
     * @return 返回结果
     */
    @ExceptionHandler(value = ServiceException.class)
    @ResponseBody
    public ResponseEntity<?> validExceptionHandler(ServiceException e, HttpServletRequest request,
                                                           HttpServletResponse response) throws IOException {
        String resolvedMessage = resolveServiceMessage(e);
        LOG.warn("业务异常：{}" , resolvedMessage);
        if (CommonStreamResult.isStreamRequest(request)) {
            int status = e.getCode();
            return CommonStreamResult.error(response, status >= 400 && status <= 599 ? status : 500,
                    resolvedMessage);
        }
        return ResponseEntity.ok(CommonResult.error(e.getCode(), resolvedMessage));
    }

    /**
     * 处理未捕获的系统异常。
     *
     * @param e 异常
     * @return 返回结果
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public CommonResult validExceptionHandler(Exception e) {
        LOG.error("系统异常：" , e);
        String resolvedMessage = resolveErrorCodeMessage(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        return CommonResult.error(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), resolvedMessage);
    }

    private String resolveValidationMessage(String message) {
        ExceptionMessageResolver resolver = exceptionMessageResolverProvider.getIfAvailable();
        return resolver == null ? message : resolver.resolveValidationMessage(message);
    }

    private String resolveServiceMessage(ServiceException exception) {
        ExceptionMessageResolver resolver = exceptionMessageResolverProvider.getIfAvailable();
        return resolver == null
                ? exception.getMessage()
                : resolver.resolveServiceMessage(exception.getCode(), exception.getMessage());
    }

    private String resolveErrorCodeMessage(ErrorCode errorCode) {
        ExceptionMessageResolver resolver = exceptionMessageResolverProvider.getIfAvailable();
        return resolver == null ? errorCode.getMsg() : resolver.resolveErrorCode(errorCode);
    }
}
