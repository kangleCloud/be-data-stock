package com.vita.system.sysRole.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.entity
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 实体类
 * @version 1.0
 */
@Data
@TableName("sys_role")
@EqualsAndHashCode(callSuper = true)
public class SysRole extends BaseEntity {

    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色编码
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 显示顺序
     */
    @TableField("role_sort")
    private Integer roleSort;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 数据范围 ALL/DEPT_AND_CHILD/DEPT_SELF/SELF/CUSTOM
     */
    @TableField("data_scope_type")
    private String dataScopeType;

    /**
     * 是否系统内置 1是 0否
     */
    @TableField("is_system")
    private Byte isSystem;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
