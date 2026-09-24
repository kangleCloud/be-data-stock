package com.vita.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流实例变量修改请求
 * @Version: 1.0
 */
@Data
public class WorkflowVariableUpdateRequest {

    @NotBlank(message = "变量键不能为空")
    @Size(max = 100, message = "变量键长度不能超过100")
    private String key;

    @NotNull(message = "变量值不能为空")
    private Object value;

    @NotBlank(message = "修改原因不能为空")
    @Size(max = 500, message = "修改原因长度不能超过500")
    private String reason;
}
