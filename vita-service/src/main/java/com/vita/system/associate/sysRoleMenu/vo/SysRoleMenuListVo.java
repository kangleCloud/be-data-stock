package com.vita.system.associate.sysRoleMenu.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleMenu.vo
 * @Author znk
 * @CreateTime 2026-03-31 16:40:21
 * @Description: 角色菜单关联表 列表 VO
 * @version 1.0
 */
@Data
public class SysRoleMenuListVo {
    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;

}
