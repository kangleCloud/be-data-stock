package com.vita.workflow.api.model;

import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.model
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 流程启动上下文
 * @Version: 1.0
 */
public record WorkflowStartContext(
        String flowCode,
        String businessId,
        Long operatorId,
        Long deptId,
        Map<String, Object> variables
) {
}
