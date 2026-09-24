package com.vita.system.sysPermission.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.vo
 * @Author znk
 * @CreateTime 2026-04-06 21:08:00
 * @Description: 权限选择框 VO
 * @version 1.0
 */
@Data
public class SysPermissionOptionVo {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 鉴权标识
     */
    private String authTag;

    /**
     * 是否选中
     */
    private boolean selected;

    /**
     * 是否禁用
     */
    private boolean disabled;
}
