package com.vita.auth.service;

import com.vita.system.sysMenu.entity.SysMenu;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.service
 * @Author: znk
 * @CreateTime: 2026-04-02  15:40:12
 * @Description: 认证公共服务接口，提供认证相关的公共方法
 * @Version: 1.0
 */
public interface IAuthCommonService {

    /**
     * 根据用户ID获取其拥有的角色编码列表。
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    List<String> getRoleCodesByUserId(Long userId);

    /**
     * 根据用户ID获取拥有的权限编码列表。
     *
     * @param userId 用户 ID
     * @return 权限code集合
     */
    List<String> getPermissionCodesByUserId(Long userId);

    /**
     * 判断用户是否是超级管理员。
     *
     * @param userId 用户 ID
     * @return 是-true/否-false
     */
    boolean isSuperAdmin(Long userId);
}
