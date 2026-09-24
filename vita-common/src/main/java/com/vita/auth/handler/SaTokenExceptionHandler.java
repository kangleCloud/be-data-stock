package com.vita.auth.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.hutool.http.HttpStatus;
import com.vita.core.CommonResult;
import com.vita.core.CommonStreamResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;


/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.handler
 * @Author: znk
 * @CreateTime: 2026-07-09  16:20:13
 * @Description: SaToken异常处理器
 * @Version: 1.0
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SaTokenExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SaTokenExceptionHandler.class);

    /**
     * 权限码异常
     *
     * @param e       异常
     * @param request 请求参数
     * @return 返回结果
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<?> handleNotPermissionException(NotPermissionException e, HttpServletRequest request,
                                                                 HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();
        LOG.error("请求地址'{}',权限码校验失败'{}'", requestURI, e.getMessage());
        if (CommonStreamResult.isStreamRequest(request)) {
            return CommonStreamResult.error(response, HttpStatus.HTTP_FORBIDDEN, "没有访问权限，请联系管理员授权");
        }
        return ResponseEntity.ok(CommonResult.error(HttpStatus.HTTP_FORBIDDEN, "没有访问权限，请联系管理员授权"));
    }

    /**
     * 角色权限异常
     *
     * @param e       异常
     * @param request 请求参数
     * @return 返回结果
     */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<?> handleNotRoleException(NotRoleException e, HttpServletRequest request,
                                                           HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();
        LOG.error("请求地址'{}',角色权限校验失败'{}'", requestURI, e.getMessage());
        if (CommonStreamResult.isStreamRequest(request)) {
            return CommonStreamResult.error(response, HttpStatus.HTTP_FORBIDDEN, "没有访问权限，请联系管理员授权");
        }
        return ResponseEntity.ok(CommonResult.error(HttpStatus.HTTP_FORBIDDEN, "没有访问权限，请联系管理员授权"));
    }

    /**
     * 认证失败
     *
     * @param e       异常
     * @param request 请求参数
     * @return 返回结果
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<?> handleNotLoginException(NotLoginException e, HttpServletRequest request,
                                                            HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();
        LOG.error("请求地址'{}',认证失败'{}',无法访问系统资源", requestURI, e.getMessage());
        if (CommonStreamResult.isStreamRequest(request)) {
            return CommonStreamResult.error(response, HttpStatus.HTTP_UNAUTHORIZED, "认证失败，无法访问系统资源");
        }
        return ResponseEntity.ok(CommonResult.error(HttpStatus.HTTP_UNAUTHORIZED, "认证失败，无法访问系统资源"));
    }
}