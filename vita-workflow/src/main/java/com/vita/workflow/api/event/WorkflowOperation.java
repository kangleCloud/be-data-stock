package com.vita.workflow.api.event;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.event
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流状态变更操作类型
 * @Version: 1.0
 */
public enum WorkflowOperation {
    START,
    PASS,
    REJECT_LAST,
    REJECT_TO,
    CANCEL,
    TRANSFER,
    DELEGATE,
    ADD_SIGN,
    REDUCE_SIGN,
    TERMINATE,
    INVALIDATE,
    ACTIVATE,
    SUSPEND,
    VARIABLE_UPDATE,
    MODIFY_HANDLER,
    DELETE,
    DELETE_HISTORY
}
