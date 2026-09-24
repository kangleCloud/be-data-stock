package com.vita.workflow.controller.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 高级任务操作请求
 * @Version: 1.0
 */
@Data
public class WorkflowTaskOperationRequest {

    @Size(max = 20, message = "办理人数量不能超过20")
    private List<Long> userIds = new ArrayList<>();

    @Size(max = 500, message = "办理意见长度不能超过500")
    private String message;

    @Size(max = 500, message = "附件引用长度不能超过500")
    private String attachmentRef;
}
