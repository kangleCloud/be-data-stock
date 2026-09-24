package com.vita.system.sysMenu.vo;

import lombok.Data;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.system.sysMenu.vo
 * @Author: znk
 * @CreateTime: 2026-04-03  15:59:42
 * @Description: 菜单路由 VO，用于前端路由展示
 * @Version: 1.0
 */
@Data
public class RouterVo {

    /**
     * 菜单名称
     */
    private String routeName;

    /**
     * 路由地址
     */
    private String routeLink;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 是否隐藏路由，当设置 true 的时候该路由不会再侧边栏出现
     */
    private boolean hidden;

    /**
     * 组件地址
     */
    private String component;


    /**
     * 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
     */
    private Boolean alwaysShow;

    /**
     * 其他元素
     */
    private MetaVo meta;

    /**
     * 子路由
     */
    private List<RouterVo> children;

}