package com.vita.system.associate.sysRoleDeptScope.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleDeptScope.entity
 * @Author znk
 * @CreateTime 2026-03-31 16:41:38
 * @Description: 角色自定义数据范围表 实体类
 * @version 1.0
 */
@Data
@TableName("sys_role_dept_scope")
@EqualsAndHashCode(callSuper = true)
public class SysRoleDeptScope extends BaseEntity {

    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 部门ID
     */
    @TableField("dept_id")
    private Long deptId;
}
