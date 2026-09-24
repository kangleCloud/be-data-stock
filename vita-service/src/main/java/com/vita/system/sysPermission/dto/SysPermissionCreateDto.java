package com.vita.system.sysPermission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 创建 DTO
 * @version 1.0
 */
@Data
public class SysPermissionCreateDto {
    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    /**
     * 权限编码
     */
    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;

    /**
     * 权限类型 MENU_ACTION/API/DATA/FIELD
     */
    @NotBlank(message = "权限类型不能为空")
    private String permissionType;

    /**
     * 关联菜单ID，可为空
     */
    private Long menuId;

    /**
     * 接口方法 GET/POST
     */
    private String apiMethod;

    /**
     * 接口路径
     */
    private String apiPath;

    /**
     * Sa-Token鉴权标识，通常等于permission_code
     */
    private String authTag;

    /**
     * 排序
     */
    @NotNull(message = "排序不能为空")
    private Integer sortNo;

    /**
     * 状态 1启用 0禁用
     */
    @NotNull(message = "状态不能为空")
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
