package com.vita.workflow.runtime;

import com.vita.workflow.api.model.WorkflowTaskCapability;
import com.vita.workflow.flow.rule.support.WorkflowAssigneeRuleBridge;
import com.vita.workflow.runtime.model.WorkflowNodePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 高级任务默认兼容与受控规则表达式测试
 * @Version: 1.0
 */
class WorkflowAdvancedPolicyTest {

    /**
     * 旧流程未配置节点扩展时只开放基础办理能力。
     */
    @Test
    void legacyPolicyShouldRejectAdvancedOperations() {
        WorkflowNodePolicy policy = WorkflowNodePolicy.legacy();

        assertThat(policy.capabilities()).containsExactlyInAnyOrder(
                WorkflowTaskCapability.PASS,
                WorkflowTaskCapability.REJECT_LAST,
                WorkflowTaskCapability.REJECT_TO);
        assertThat(policy.allows(WorkflowTaskCapability.TRANSFER)).isFalse();
        assertThat(policy.allows(WorkflowTaskCapability.TERMINATE)).isFalse();
        assertThat(policy.allows(WorkflowTaskCapability.COPY)).isFalse();
    }

    /**
     * 规则表达式只能由平台固定桥接 Bean、规则编码和参数名生成。
     */
    @Test
    void assigneeExpressionShouldUseControlledBridge() {
        String expression = WorkflowAssigneeRuleBridge.buildExpression(
                "DEPT_LEADER", List.of("deptId", "amount"));

        assertThat(expression).isEqualTo(
                "#{@workflowAssigneeRuleBridge.resolve('DEPT_LEADER',#deptId,#amount)}");
        assertThat(expression).doesNotContain("T(", "Runtime", "class");
    }
}
