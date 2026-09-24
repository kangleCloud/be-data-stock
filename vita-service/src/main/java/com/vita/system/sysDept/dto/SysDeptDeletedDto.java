package com.vita.system.sysDept.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysDept.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:29:38
 * @Description: 部门表 删除 DTO
 * @version 1.0
 */
@Data
public class SysDeptDeletedDto {

    /**
     * 主键ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;
}
