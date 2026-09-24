package com.vita.log.interceptor;

import cn.dev33.satoken.exception.SaTokenContextException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.vita.auth.constant.AuthConstants;
import com.vita.auth.context.LoginUserInfoContextLoader;
import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.interceptor
 * @Author: znk
 * @CreateTime: 2026-03-12  00:00:00
 * @Description: 请求级上下文拦截器
 * @Version: 1.0
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RequestTraceInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_KEY = "requestId";

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ObjectProvider<LoginUserInfoContextLoader> loginUserInfoContextLoaderProvider;

    public RequestTraceInterceptor(ObjectProvider<LoginUserInfoContextLoader> loginUserInfoContextLoaderProvider) {
        this.loginUserInfoContextLoaderProvider = loginUserInfoContextLoaderProvider;
    }

    /**
     * 预处理当前请求上下文。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器对象
     * @return true 表示继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        clearRequestContext();

        String requestId = resolveRequestId(request);
        MDC.put(REQUEST_ID_KEY, requestId);
        populateLoginUserContext();
        return true;
    }

    /**
     * 清理当前请求上下文。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器对象
     * @param ex       异常信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        clearRequestContext();
    }

    /**
     * 解析当前请求标识。
     *
     * @param request 请求对象
     * @return 请求 ID
     */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (CharSequenceUtil.isBlank(requestId)) {
            requestId = request.getHeader(REQUEST_ID_KEY);
        }
        return CharSequenceUtil.isNotBlank(requestId) ? requestId : IdUtil.fastSimpleUUID();
    }

    /**
     * 准备当前线程的登录用户上下文。
     */
    private void populateLoginUserContext() {
        try {
            if (!StpUtil.isLogin()) {
                return;
            }
        } catch (SaTokenContextException ex) {
            return;
        }

        LoginUserInfoModel loginUserInfoModel = resolveStoredLoginUserInfo(StpUtil.getSession().get(AuthConstants.LOGIN_USER_INFO));
        if (loginUserInfoModel != null) {
            LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);
            return;
        }

        restoreLoginUserContext();
    }

    /**
     * 解析 Session 中缓存的登录用户快照。
     *
     * @param sessionValue Session 缓存值
     * @return 登录用户快照
     */
    private LoginUserInfoModel resolveStoredLoginUserInfo(Object sessionValue) {
        if (sessionValue instanceof LoginUserInfoModel model) {
            return model;
        }
        return sessionValue != null ? BeanUtil.toBean(sessionValue, LoginUserInfoModel.class) : null;
    }

    /**
     * Session 未命中登录用户快照时，根据 loginId 重建并回写 Session 与线程上下文。
     */
    private void restoreLoginUserContext() {
        Long userId = resolveUserId(StpUtil.getLoginIdDefaultNull());
        if (userId == null) {
            return;
        }
        LoginUserInfoContextLoader loader = loginUserInfoContextLoaderProvider.getIfAvailable();
        if (loader == null) {
            return;
        }
        LoginUserInfoModel loginUserInfoModel = loader.loadByUserId(userId);
        if (loginUserInfoModel == null) {
            return;
        }
        StpUtil.getSession().set(AuthConstants.LOGIN_USER_INFO, loginUserInfoModel);
        LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);
    }

    /**
     * 将任意登录 ID 安全转换为 Long。
     *
     * @param loginId 登录 ID
     * @return 用户 ID；无法转换时返回 null
     */
    private Long resolveUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 清理线程中的请求上下文信息。
     */
    private void clearRequestContext() {
        LoginUserInfoModelContext.removeLoginUserInfo();
        MDC.remove(REQUEST_ID_KEY);
    }
}
