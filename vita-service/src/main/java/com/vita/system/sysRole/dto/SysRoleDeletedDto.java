package com.vita.system.sysRole.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.dto
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 删除 DTO
 * @version 1.0
 */
@Data
public class SysRoleDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
