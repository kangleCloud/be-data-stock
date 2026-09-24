package com.vita.system.sysRole.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.dto
 * @Author znk
 * @CreateTime 2026-04-06 21:08:00
 * @Description: 角色授权单个权限 DTO
 * @version 1.0
 */
@Data
public class SysRoleAuthPermissionDto {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 权限ID
     */
    @NotNull(message = "权限ID不能为空")
    private Long permissionId;
}
