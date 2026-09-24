package com.vita.workflow.runtime.dto;

import com.vita.core.page.PageRequest;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.dto
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流定义分页查询参数
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowDefinitionSearchDto extends PageRequest {

    @Size(max = 100, message = "关键字长度不能超过100")
    private String keyword;

    @Size(max = 64, message = "流程分类长度不能超过64")
    private String category;

    private Integer isPublish;

    private Integer activityStatus;
}
