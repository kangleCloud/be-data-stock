package com.vita.workflow.flow.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工作流分类删除请求。
 *
 * @author znk
 */
@Data
public class WorkflowCategoryDeletedDto {

    /**
     * 分类 ID。
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;
}
