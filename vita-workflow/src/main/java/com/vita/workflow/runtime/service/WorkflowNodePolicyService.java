package com.vita.workflow.runtime.service;

import com.vita.workflow.runtime.model.WorkflowNodePolicy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.service
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流节点扩展策略服务
 * @Version: 1.0
 */
public interface WorkflowNodePolicyService {

    /**
     * 查询指定定义节点的运行时策略。
     *
     * @param definitionId 流程定义ID
     * @param nodeCode 节点编码
     * @return 节点策略
     */
    WorkflowNodePolicy getPolicy(Long definitionId, String nodeCode);
}
