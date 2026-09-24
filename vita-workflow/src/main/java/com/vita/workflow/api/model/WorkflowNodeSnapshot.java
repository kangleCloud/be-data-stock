package com.vita.workflow.api.model;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.model
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流节点只读快照
 * @Version: 1.0
 */
public record WorkflowNodeSnapshot(
        String nodeCode,
        String nodeName,
        Integer nodeType,
        boolean assigneeSelectionRequired
) {
}
