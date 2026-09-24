package com.vita.workflow.runtime.support;

import com.vita.workflow.api.event.WorkflowChangedEvent;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.support
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流事务提交后事件发布器
 * @Version: 1.0
 */
@Component
@ConditionalOnWorkflowEnabled
public class WorkflowAfterCommitEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowAfterCommitEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    public WorkflowAfterCommitEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 在当前事务成功提交后发布事件。
     *
     * @param event 工作流事件
     */
    public void publish(WorkflowChangedEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSafely(event);
                }
            });
            return;
        }
        publishSafely(event);
    }

    private void publishSafely(WorkflowChangedEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception ex) {
            LOG.error("工作流提交后事件处理失败, eventId={}, flowCode={}, instanceId={}",
                    event.eventId(), event.flowCode(), event.instanceId(), ex);
        }
    }
}
