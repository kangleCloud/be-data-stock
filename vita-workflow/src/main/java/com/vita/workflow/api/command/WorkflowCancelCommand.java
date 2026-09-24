package com.vita.workflow.api.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.command
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 未审批流程撤回命令
 * @Version: 1.0
 */
@Data
public class WorkflowCancelCommand {

    @NotNull(message = "流程实例ID不能为空")
    private Long instanceId;

    @Size(max = 500, message = "撤销原因长度不能超过500")
    private String message;
}
