package com.vita.system.sysLoginLog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysLoginLog.dto
 * @Author znk
 * @CreateTime 2026-03-21 00:20:48
 * @Description: 系统访问记录 删除 DTO
 * @version 1.0
 */
@Data
public class SysLoginLogDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
