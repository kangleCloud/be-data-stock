package com.vita.system.sysRole.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.dto
 * @Author znk
 * @CreateTime 2026-04-06 23:35:00
 * @Description: 角色授权单个用户 DTO
 * @version 1.0
 */
@Data
public class SysRoleAuthUserDto {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
