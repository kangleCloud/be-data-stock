package com.vita.workflow.controller.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流撤回请求
 * @Version: 1.0
 */
@Data
public class WorkflowCancelRequest {

    @Size(max = 500, message = "撤回原因长度不能超过500")
    private String message;
}
