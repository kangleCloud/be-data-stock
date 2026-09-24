package com.vita.auth.context;

import com.vita.auth.model.LoginUserInfoModel;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.context
 * @Author: znk
 * @CreateTime: 2026-03-06  21:12:12
 * @Description: 登录用户信息模型上下文，提供全局访问登录用户信息的方式
 * @Version: 1.0
 */
public final class LoginUserInfoModelContext {

    private static final ThreadLocal<LoginUserInfoModel> LOGIN_USER_INFO_THREAD_LOCAL = new ThreadLocal<>();

    private LoginUserInfoModelContext() {
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUserInfoModel getLoginUserInfo() {
        return LOGIN_USER_INFO_THREAD_LOCAL.get();
    }

    /**
     * 获取当前登录用户；若不存在则抛出未登录异常。
     *
     * @return 当前登录用户
     */
    public static LoginUserInfoModel getRequiredLoginUserInfo() {
        LoginUserInfoModel model = getLoginUserInfo();
        if (model == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        return model;
    }

    /**
     * 判断当前线程是否存在登录用户上下文。
     *
     * @return true 已登录 / false 未登录
     */
    public static boolean hasLoginUserInfo() {
        return getLoginUserInfo() != null;
    }

    /**
     * 判断当前线程是否已登录。
     *
     * @return true 已登录 / false 未登录
     */
    public static boolean isLogin() {
        return hasLoginUserInfo();
    }

    /**
     * 设置当前登录用户信息
     *
     * @param model 登录用户信息模型
     */
    public static void setLoginUserInfo(LoginUserInfoModel model) {
        LOGIN_USER_INFO_THREAD_LOCAL.set(model);
    }

    /**
     * 清除当前登录用户信息
     */
    public static void removeLoginUserInfo() {
        LOGIN_USER_INFO_THREAD_LOCAL.remove();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 登录用户ID
     */
    public static Long getLoginUserId() {
        LoginUserInfoModel model = getLoginUserInfo();
        return model != null ? model.getId() : null;
    }

    /**
     * 获取当前登录用户ID；若不存在则抛出未登录异常。
     *
     * @return 登录用户ID
     */
    public static Long getRequiredLoginUserId() {
        LoginUserInfoModel model = getRequiredLoginUserInfo();
        if (model.getId() == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        return model.getId();
    }

    /**
     * 获取当前登录用户名。
     *
     * @return 登录用户名
     */
    public static String getLoginUsername() {
        LoginUserInfoModel model = getLoginUserInfo();
        return model != null ? model.getUsername() : null;
    }

    /**
     * 获取当前登录用户名；若不存在则抛出未登录异常。
     *
     * @return 登录用户名
     */
    public static String getRequiredLoginUsername() {
        LoginUserInfoModel model = getRequiredLoginUserInfo();
        if (model.getUsername() == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        return model.getUsername();
    }
}
