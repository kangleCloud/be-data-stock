package com.vita.system.associate.sysRoleMenu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleMenu.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:40:21
 * @Description: 角色菜单关联表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleMenuUpdateDto extends SysRoleMenuCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
