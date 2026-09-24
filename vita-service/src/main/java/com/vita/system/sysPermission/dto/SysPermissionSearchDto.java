package com.vita.system.sysPermission.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysPermissionSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限类型
     */
    private String permissionType;

    /**
     * 菜单ID
     */
    private Long menuId;

    /**
     * 状态
     */
    private Byte status;
}
