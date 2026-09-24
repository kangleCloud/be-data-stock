package com.vita.system.sysRole.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.vo
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 列表 VO
 * @version 1.0
 */
@Data
public class SysRoleListVo {
    /**
     * 角色 ID
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
     * 显示顺序
     */
    private Integer roleSort;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 数据范围 ALL/DEPT_AND_CHILD/DEPT_SELF/SELF/CUSTOM
     */
    private String dataScopeType;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 备注
     */
    private String remark;

}
