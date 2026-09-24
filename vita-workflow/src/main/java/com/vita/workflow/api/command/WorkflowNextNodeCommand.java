package com.vita.workflow.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.command
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 下一节点预览命令
 * @Version: 1.0
 */
@Data
public class WorkflowNextNodeCommand {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    private Map<String, Object> variables = new HashMap<>();
}
