package com.vita.system.sysMenu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 创建 DTO
 */
@Data
public class SysMenuCreateDto {
    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    @Size(min = 0, max = 50, message = "菜单名称长度不能超过50个字符")
    private String menuName;

    /**
     * 类型 CONTENTS/MENU/LINK
     */
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    /**
     * 路由名称
     */
    private String routeName;

    /**
     * 路由地址
     */
    @Size(min = 0, max = 200, message = "路由地址不能超过200个字符")
    private String routeLink;

    /**
     * 组件路径
     */
    @Size(min = 0, max = 200, message = "组件路径不能超过255个字符")
    private String componentPath;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    @Size(min = 0, max = 999, message = "排序必须在0到999之间")
    private Integer sortNo;

    /**
     * 是否显示 1是 0否
     */
    private Byte visible;

    /**
     * 是否缓存 1是 0否
     */
    private Byte isCache;

    /**
     * 是否总显示
     */
    private Byte alwaysShow;

    /**
     * 是否外链
     */
    private Byte isExternal;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 备注
     */
    private String remark;

}
