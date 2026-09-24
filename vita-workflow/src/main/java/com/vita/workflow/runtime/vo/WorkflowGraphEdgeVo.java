package com.vita.workflow.runtime.vo;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流图连线安全投影
 * @Version: 1.0
 */
@Data
public class WorkflowGraphEdgeVo {

    private String sourceNodeCode;

    private String targetNodeCode;

    private String coordinate;

    private String state;
}
