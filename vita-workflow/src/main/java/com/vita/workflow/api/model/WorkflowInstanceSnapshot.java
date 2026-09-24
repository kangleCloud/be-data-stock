package com.vita.workflow.api.model;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.model
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 与引擎无关的流程实例快照
 * @Version: 1.0
 */
@Data
public class WorkflowInstanceSnapshot {

    private Long definitionId;
    private Long instanceId;
    private Long taskId;
    private String businessId;
    private String flowCode;
    private String flowName;
    private String nodeCode;
    private String nodeName;
    private String flowStatus;
}
