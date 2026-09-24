package com.vita.workflow.flow.category.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.category.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流分类更新请求，分类编码创建后不可修改
 * @Version: 1.0
 */
@Data
public class WorkflowCategoryUpdateDto {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100")
    private String categoryName;

    @Size(max = 64, message = "父分类编码长度不能超过64")
    private String parentCode;

    @NotNull(message = "排序不能为空")
    private Integer sortNo;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Byte status;
}
