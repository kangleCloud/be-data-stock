package com.vita.auth.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 异步完成和错误分派沿用首次请求的鉴权结果；其他分派照常鉴权。
 */
final class RequestDispatchAuthInterceptor implements AsyncHandlerInterceptor {

    private final HandlerInterceptor delegate;

    /**
     * 包装登录校验拦截器，在异步和错误分派时避免重复校验。
     *
     * @param delegate 实际执行登录校验的拦截器
     */
    RequestDispatchAuthInterceptor(HandlerInterceptor delegate) {
        this.delegate = delegate;
    }

    /**
     * 首次请求及其他普通分派交由原拦截器校验；异步和错误分派沿用首次请求的结果。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 当前处理器
     * @return 是否继续处理请求
     * @throws Exception 原拦截器抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (request.getDispatcherType() == DispatcherType.ASYNC
                || request.getDispatcherType() == DispatcherType.ERROR) {
            return true;
        }
        return delegate.preHandle(request, response, handler);
    }

    /**
     * 将处理器执行后的回调交给原拦截器。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 当前处理器
     * @param modelAndView 处理器返回的视图，可为空
     * @throws Exception 原拦截器抛出的异常
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) throws Exception {
        delegate.postHandle(request, response, handler, modelAndView);
    }

    /**
     * 将请求完成回调交给原拦截器。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 当前处理器
     * @param exception 处理请求时发生的异常，可为空
     * @throws Exception 原拦截器抛出的异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                @Nullable Exception exception) throws Exception {
        delegate.afterCompletion(request, response, handler, exception);
    }

    /**
     * 原拦截器支持异步回调时，转发异步处理开始事件。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 当前处理器
     * @throws Exception 原拦截器抛出的异常
     */
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
                                               Object handler) throws Exception {
        if (delegate instanceof AsyncHandlerInterceptor asyncDelegate) {
            asyncDelegate.afterConcurrentHandlingStarted(request, response, handler);
        }
    }
}
