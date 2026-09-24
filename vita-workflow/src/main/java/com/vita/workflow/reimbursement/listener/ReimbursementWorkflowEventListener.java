package com.vita.workflow.reimbursement.listener;

import com.vita.workflow.api.event.WorkflowChangedEvent;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.reimbursement.service.WorkflowReimbursementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.listener
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销工作流提交后状态同步监听器
 * @Version: 1.0
 */
@Component
@ConditionalOnWorkflowEnabled
public class ReimbursementWorkflowEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ReimbursementWorkflowEventListener.class);

    private final WorkflowReimbursementService reimbursementService;

    public ReimbursementWorkflowEventListener(WorkflowReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    /**
     * 同步报销业务状态，失败只记录并由管理员重同步补偿。
     *
     * @param event 工作流变更事件
     */
    @EventListener
    public void onWorkflowChanged(WorkflowChangedEvent event) {
        try {
            reimbursementService.syncFromEvent(event);
        } catch (Exception ex) {
            LOG.error("报销工作流状态同步失败, eventId={}, businessId={}",
                    event.eventId(), event.businessId(), ex);
        }
    }
}
