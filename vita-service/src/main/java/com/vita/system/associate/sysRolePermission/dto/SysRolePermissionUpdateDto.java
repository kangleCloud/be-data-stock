package com.vita.system.associate.sysRolePermission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRolePermission.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:40:54
 * @Description: 角色权限关联表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRolePermissionUpdateDto extends SysRolePermissionCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
