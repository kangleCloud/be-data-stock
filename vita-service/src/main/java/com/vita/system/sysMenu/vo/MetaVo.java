package com.vita.system.sysMenu.vo;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.system.sysMenu.vo
 * @Author: znk
 * @CreateTime: 2026-04-06  17:24:33
 * @Description: 路由显示信息
 * @Version: 1.0
 */
@Data
public class MetaVo {
    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 图标
     */
    private String icon;


    /**
     * 是否缓存 1是 0否
     */
    private Byte isCache;
}
