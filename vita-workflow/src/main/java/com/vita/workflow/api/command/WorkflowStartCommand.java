package com.vita.workflow.api.command;

import com.vita.workflow.api.model.WorkflowBusinessMetadata;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.command
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 流程启动命令
 * @Version: 1.0
 */
@Data
public class WorkflowStartCommand {

    @NotBlank(message = "流程编码不能为空")
    @Size(max = 100, message = "流程编码长度不能超过100")
    private String flowCode;

    @NotBlank(message = "业务ID不能为空")
    @Size(max = 40, message = "业务ID长度不能超过40")
    private String businessId;

    private Map<String, Object> variables = new HashMap<>();

    @Valid
    private WorkflowBusinessMetadata metadata;
}
