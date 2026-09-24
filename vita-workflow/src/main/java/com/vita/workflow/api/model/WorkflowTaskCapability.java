package com.vita.workflow.api.model;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.model
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流节点任务能力
 * @Version: 1.0
 */
public enum WorkflowTaskCapability {
    PASS,
    REJECT_LAST,
    REJECT_TO,
    TRANSFER,
    DELEGATE,
    ADD_SIGN,
    REDUCE_SIGN,
    TERMINATE,
    COPY,
    SELECT_NEXT_ASSIGNEE
}
