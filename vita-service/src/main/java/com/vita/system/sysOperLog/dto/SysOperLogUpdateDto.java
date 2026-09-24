package com.vita.system.sysOperLog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysOperLog.dto
 * @Author znk
 * @CreateTime 2026-03-12 21:45:20
 * @Description: 系统操作日志表 更新 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysOperLogUpdateDto extends SysOperLogCreateDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
