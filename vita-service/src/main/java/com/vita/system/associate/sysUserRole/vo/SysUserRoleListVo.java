package com.vita.system.associate.sysUserRole.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysUserRole.vo
 * @Author znk
 * @CreateTime 2026-03-31 16:39:20
 * @Description: 用户角色关联表 列表 VO
 * @version 1.0
 */
@Data
public class SysUserRoleListVo {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;

}
