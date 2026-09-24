package com.vita.auth.service;

import com.vita.auth.model.LoginUserInfoModel;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.service
 * @Author: Codex
 * @CreateTime: 2026-03-21
 * @Description: 认证令牌上下文服务
 * @Version: 1.0
 */
public interface IAuthTokenService {

    /**
     * 建立当前用户的登录态。
     *
     * @param userId 用户ID
     */
    void login(Long userId);

    /**
     * 校验当前请求是否已经登录。
     */
    void checkLogin();

    /**
     * 注销当前登录态。
     */
    void logout();

    /**
     * 将登录用户信息写入认证上下文。
     *
     * @param loginUserInfoModel 登录用户信息
     */
    void storeLoginUserInfo(LoginUserInfoModel loginUserInfoModel);

    /**
     * 读取认证上下文中的登录用户信息。
     *
     * @return 登录用户信息对象
     */
    Object getStoredLoginUserInfo();

    /**
     * 将登录用户角色码集合写入认证上下文。
     *
     * @param roleCodes 角色码集合
     */
    void storeLoginRoleCodes(List<String> roleCodes);

    /**
     * 读取认证上下文中的角色码集合。
     *
     * @return 角色码集合
     */
    Object getStoredLoginRoleCodes();

    /**
     * 将登录用户权限码集合写入认证上下文。
     *
     * @param permissionCodes 权限码集合
     */
    void storeLoginPermissionCodes(List<String> permissionCodes);

    /**
     * 读取认证上下文中的权限码集合。
     *
     * @return 权限码集合
     */
    Object getStoredLoginPermissionCodes();

    /**
     * 获取当前 token 值。
     *
     * @return token 值
     */
    String getTokenValue();

    /**
     * 获取 token 剩余有效期。
     *
     * @return 剩余有效期，单位秒
     */
    Long getTokenTimeout();

    /**
     * 获取 token 名称。
     *
     * @return token 名称
     */
    String getTokenName();

    /**
     * 获取 token 前缀。
     *
     * @return token 前缀
     */
    String getTokenPrefix();

    /**
     * 获取当前登录用户ID，未登录时返回空。
     *
     * @return 登录用户ID
     */
    Object getLoginId();
}
