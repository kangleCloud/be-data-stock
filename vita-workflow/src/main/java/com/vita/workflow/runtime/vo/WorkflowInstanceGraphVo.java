package com.vita.workflow.runtime.vo;

import lombok.Data;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流实例图和轨迹聚合结果
 * @Version: 1.0
 */
@Data
public class WorkflowInstanceGraphVo {

    private WorkflowInstanceVo instance;

    private List<WorkflowGraphNodeVo> nodes;

    private List<WorkflowGraphEdgeVo> edges;

    private List<WorkflowHistoryVo> history;
}
