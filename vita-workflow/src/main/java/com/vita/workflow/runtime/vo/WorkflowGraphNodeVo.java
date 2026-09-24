package com.vita.workflow.runtime.vo;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流图节点安全投影
 * @Version: 1.0
 */
@Data
public class WorkflowGraphNodeVo {

    private String nodeCode;

    private String nodeName;

    private Integer nodeType;

    private String coordinate;

    private String state;
}
