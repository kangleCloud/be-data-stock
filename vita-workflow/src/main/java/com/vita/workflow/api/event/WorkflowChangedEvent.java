package com.vita.workflow.api.event;

import java.time.Instant;
import java.util.UUID;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.event
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流事务提交后状态变更事件
 * @Version: 1.0
 */
public record WorkflowChangedEvent(
        UUID eventId,
        Instant occurredAt,
        WorkflowOperation operation,
        String flowCode,
        Long definitionId,
        Long instanceId,
        String businessId,
        Long taskId,
        Long operatorId,
        String sourceNodeCode,
        String targetNodeCode,
        String flowStatus
) {
}
