package com.vita.workflow.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流激活状态请求
 * @Version: 1.0
 */
@Data
public class WorkflowActivityRequest {

    @NotNull(message = "激活状态不能为空")
    private Boolean active;
}
