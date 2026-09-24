package com.vita.system.sysIpBlock.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.dto
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 删除 DTO
 * @version 1.0
 */
@Data
public class SysIpBlockDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
