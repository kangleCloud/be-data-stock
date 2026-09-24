package com.vita.auth.vo;

import lombok.Data;

/**
 * 认证上下文菜单响应。
 *
 * @author znk
 */
@Data
public class AuthMenuVo {

    private Long id;

    private Long parentId;

    private String menuName;

    private String menuType;

    private String routeName;

    private String routeLink;

    private String componentPath;

    private String icon;

    private Integer sortNo;

    private Byte visible;

    private Byte isCache;

    private Byte alwaysShow;

    private Byte isExternal;
}
