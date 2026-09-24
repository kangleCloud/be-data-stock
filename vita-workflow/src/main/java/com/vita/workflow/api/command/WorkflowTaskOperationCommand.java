package com.vita.workflow.api.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.command
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 高级任务操作命令
 * @Version: 1.0
 */
@Data
public class WorkflowTaskOperationCommand {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Size(max = 20, message = "办理人数量不能超过20")
    private List<Long> userIds = new ArrayList<>();

    @Size(max = 500, message = "办理意见长度不能超过500")
    private String message;

    @Size(max = 500, message = "附件引用长度不能超过500")
    private String attachmentRef;
}
