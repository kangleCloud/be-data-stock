package com.vita.system.sysRole.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.dto
 * @Author znk
 * @CreateTime 2026-04-06 23:35:00
 * @Description: 角色批量授权用户 DTO
 * @version 1.0
 */
@Data
public class SysRoleAuthUsersDto {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 用户ID列表
     */
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;
}
