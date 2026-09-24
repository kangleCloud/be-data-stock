package com.vita.system.sysPermission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 删除 DTO
 * @version 1.0
 */
@Data
public class SysPermissionDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
