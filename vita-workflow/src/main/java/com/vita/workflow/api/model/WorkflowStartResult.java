package com.vita.workflow.api.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.model
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 流程启动结果
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowStartResult extends WorkflowInstanceSnapshot {
}
