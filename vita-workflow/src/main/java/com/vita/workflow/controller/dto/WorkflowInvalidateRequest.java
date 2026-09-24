package com.vita.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流实例作废请求
 * @Version: 1.0
 */
@Data
public class WorkflowInvalidateRequest {

    @NotBlank(message = "作废原因不能为空")
    @Size(max = 500, message = "作废原因长度不能超过500")
    private String message;
}
