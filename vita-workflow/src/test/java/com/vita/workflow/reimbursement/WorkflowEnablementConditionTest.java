package com.vita.workflow.reimbursement;

import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.reimbursement.listener.ReimbursementWorkflowEventListener;
import com.vita.workflow.reimbursement.service.impl.WorkflowReimbursementServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 报销工作流总开关注册条件测试
 * @Version: 1.0
 */
class WorkflowEnablementConditionTest {

    /**
     * 报销服务和事件监听器必须由工作流模块显式开启。
     */
    @Test
    void reimbursementComponentsShouldRequireExplicitEnablement() {
        assertRequiresExplicitEnablement(WorkflowReimbursementServiceImpl.class);
        assertRequiresExplicitEnablement(ReimbursementWorkflowEventListener.class);
    }

    private void assertRequiresExplicitEnablement(Class<?> componentType) {
        ConditionalOnWorkflowEnabled condition = componentType.getAnnotation(ConditionalOnWorkflowEnabled.class);
        assertThat(condition).as(componentType.getSimpleName()).isNotNull();
    }
}
