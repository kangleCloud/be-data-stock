package com.vita.system.sysRole.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.vo
 * @Author znk
 * @CreateTime 2026-04-06 23:35:00
 * @Description: 角色选择框 VO
 * @version 1.0
 */
@Data
public class SysRoleOptionVo {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 是否选中
     */
    private boolean selected;

    /**
     * 是否禁用
     */
    private boolean disabled;
}
