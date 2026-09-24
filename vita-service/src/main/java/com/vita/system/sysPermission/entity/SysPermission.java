package com.vita.system.sysPermission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.entity
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 实体类
 * @version 1.0
 */
@Data
@TableName("sys_permission")
@EqualsAndHashCode(callSuper = true)
public class SysPermission extends BaseEntity {

    /**
     * 权限名称
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 权限编码
     */
    @TableField("permission_code")
    private String permissionCode;

    /**
     * 权限类型 MENU_ACTION/API/DATA/FIELD
     */
    @TableField("permission_type")
    private String permissionType;

    /**
     * 关联菜单ID，可为空
     */
    @TableField("menu_id")
    private Long menuId;

    /**
     * 接口方法 GET/POST
     */
    @TableField("api_method")
    private String apiMethod;

    /**
     * 接口路径
     */
    @TableField("api_path")
    private String apiPath;

    /**
     * Sa-Token鉴权标识，通常等于permission_code
     */
    @TableField("auth_tag")
    private String authTag;

    /**
     * 排序
     */
    @TableField("sort_no")
    private Integer sortNo;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Byte status;

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
