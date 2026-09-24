package com.vita.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.vita.auth.constant.AuthConstants;
import com.vita.auth.service.IAuthCommonService;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.service.impl
 * @Author: znk
 * @CreateTime: 2026-04-02  15:43:20
 * @Description: 认证公共服务实现类，提供认证相关的公共方法的具体实现
 * @Version: 1.0
 */
@Service
public class AuthCommonServiceImpl implements IAuthCommonService {

    private final ISysUserService sysUserService;
    private final ISysUserRoleService sysUserRoleService;
    private final ISysRolePermissionService sysRolePermissionService;
    private final ISysPermissionService sysPermissionService;
    private final ISysRoleService sysRoleService;

    public AuthCommonServiceImpl(ISysUserService sysUserService,
                                 ISysUserRoleService sysUserRoleService,
                                 ISysRolePermissionService sysRolePermissionService,
                                 ISysPermissionService sysPermissionService,
                                 ISysRoleService sysRoleService) {
        this.sysUserService = sysUserService;
        this.sysUserRoleService = sysUserRoleService;
        this.sysRolePermissionService = sysRolePermissionService;
        this.sysPermissionService = sysPermissionService;
        this.sysRoleService = sysRoleService;
    }

    /**
     * 根据用户ID获取其拥有的角色编码列表。
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        SysUser user = getActiveUser(userId);
        if (user == null) {
            return CollUtil.newArrayList();
        }
        if (CommonStatusEnum.isEnabled(user.getIsSuperAdmin())) {
            return CollUtil.newArrayList(AuthConstants.SUPER_ADMIN_ROLE_CODE);
        }
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        return sysRoleService.getRoleCodesByIds(roleIds, CommonStatusEnum.ENABLED.getCode());
    }

    /**
     * 根据用户ID获取拥有的权限编码列表。
     *
     * @param userId 用户 ID
     * @return 权限code集合
     */
    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        SysUser user = getActiveUser(userId);
        if (user == null) {
            return CollUtil.newArrayList();
        }
        if (CommonStatusEnum.isEnabled(user.getIsSuperAdmin())) {
            return CollUtil.newArrayList(AuthConstants.ALL_PERMISSION_CODE);
        }
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        List<Long> permissionIds = sysRolePermissionService.getPermissionIdsByRoleIds(roleIds);
        if (CollUtil.isEmpty(permissionIds)) {
            return CollUtil.newArrayList();
        }
        return sysPermissionService.getPermissionCodesByIds(permissionIds, CommonStatusEnum.ENABLED.getCode());
    }


    /**
     * 判断用户是否是超级管理员。
     *
     * @param userId 用户 ID
     * @return 是-true/否-false
     */
    @Override
    public boolean isSuperAdmin(Long userId) {
        SysUser user = getActiveUser(userId);
        return user != null && CommonStatusEnum.isEnabled(user.getIsSuperAdmin());
    }

    /**
     * 根据 userId 查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    private SysUser getActiveUser(Long userId) {
        return sysUserService.getActiveById(userId);
    }
}
