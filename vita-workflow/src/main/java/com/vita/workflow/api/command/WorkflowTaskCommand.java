package com.vita.workflow.api.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.command
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 流程任务办理命令
 * @Version: 1.0
 */
@Data
public class WorkflowTaskCommand {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Size(max = 500, message = "办理意见长度不能超过500")
    private String message;

    private Map<String, Object> variables = new HashMap<>();

    @Size(max = 500, message = "附件引用长度不能超过500")
    private String attachmentRef;

    private Map<String, List<Long>> nextAssignees = new HashMap<>();

    @Size(max = 20, message = "抄送人数量不能超过20")
    private List<Long> copyUserIds = new ArrayList<>();
}
