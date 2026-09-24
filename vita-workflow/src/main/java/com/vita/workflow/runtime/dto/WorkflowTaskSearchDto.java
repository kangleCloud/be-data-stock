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
 * @Description: 工作流任务分页查询参数
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowTaskSearchDto extends PageRequest {

    @Size(max = 100, message = "关键字长度不能超过100")
    private String keyword;

    @Size(max = 100, message = "流程编码长度不能超过100")
    private String flowCode;
}
