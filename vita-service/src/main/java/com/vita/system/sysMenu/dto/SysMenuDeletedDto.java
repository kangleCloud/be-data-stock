package com.vita.system.sysMenu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 删除 DTO
 * @version 1.0
 */
@Data
public class SysMenuDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
