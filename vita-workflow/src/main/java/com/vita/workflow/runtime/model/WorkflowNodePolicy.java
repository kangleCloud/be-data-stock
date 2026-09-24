package com.vita.workflow.runtime.model;

import com.vita.workflow.api.model.WorkflowTaskCapability;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.model
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流节点运行时策略
 * @Version: 1.0
 */
public record WorkflowNodePolicy(
        Set<WorkflowTaskCapability> capabilities,
        Set<Long> defaultCopyUserIds,
        boolean assigneeSelectionRequired
) {

    /**
     * 创建旧流程兼容策略。
     *
     * @return 仅开放基础办理能力的节点策略
     */
    public static WorkflowNodePolicy legacy() {
        Set<WorkflowTaskCapability> capabilities = new LinkedHashSet<>();
        capabilities.add(WorkflowTaskCapability.PASS);
        capabilities.add(WorkflowTaskCapability.REJECT_LAST);
        capabilities.add(WorkflowTaskCapability.REJECT_TO);
        return new WorkflowNodePolicy(
                Collections.unmodifiableSet(capabilities),
                Collections.emptySet(),
                false);
    }

    /**
     * 判断节点是否开放指定能力。
     *
     * @param capability 任务能力
     * @return true表示开放
     */
    public boolean allows(WorkflowTaskCapability capability) {
        return capabilities.contains(capability);
    }
}
