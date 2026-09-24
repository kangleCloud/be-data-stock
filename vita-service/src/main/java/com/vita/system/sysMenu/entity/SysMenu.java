package com.vita.system.sysMenu.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.entity
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 实体类
 */
@Data
@TableName("sys_menu")
@EqualsAndHashCode(callSuper = true)
public class SysMenu extends BaseEntity {

    /**
     * 父菜单ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    private String menuName;

    /**
     * 类型 CONTENTS/MENU/LINK
     */
    @TableField("menu_type")
    private String menuType;

    /**
     * 路由名称
     */
    @TableField("route_name")
    private String routeName;

    /**
     * 路由地址
     */
    @TableField("route_link")
    private String routeLink;

    /**
     * 组件路径
     */
    @TableField("component_path")
    private String componentPath;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 排序
     */
    @TableField("sort_no")
    private Integer sortNo;

    /**
     * 是否显示 1是 0否
     */
    @TableField("visible")
    private Byte visible;

    /**
     * 是否缓存 1是 0否
     */
    @TableField("is_cache")
    private Byte isCache;

    /**
     * 是否总显示
     */
    @TableField("always_show")
    private Byte alwaysShow;

    /**
     * 是否外链
     */
    @TableField("is_external")
    private Byte isExternal;

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

    /**
     * 子菜单
     */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<SysMenu>();
}
