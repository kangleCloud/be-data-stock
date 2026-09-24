package com.vita.workflow.controller.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流任务操作请求
 * @Version: 1.0
 */
@Data
public class WorkflowActionRequest {

    @Size(max = 500, message = "办理意见长度不能超过500")
    private String message;

    private Map<String, Object> variables = new HashMap<>();

    @Size(max = 500, message = "附件引用长度不能超过500")
    private String attachmentRef;

    private Map<String, List<Long>> nextAssignees = new HashMap<>();

    @Size(max = 20, message = "抄送人数量不能超过20")
    private List<Long> copyUserIds = new ArrayList<>();
}
