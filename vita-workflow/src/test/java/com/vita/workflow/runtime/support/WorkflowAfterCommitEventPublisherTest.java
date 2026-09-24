package com.vita.workflow.runtime.support;

import com.vita.workflow.api.event.WorkflowChangedEvent;
import com.vita.workflow.api.event.WorkflowOperation;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.support
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流事务提交后事件发布器测试
 * @Version: 1.0
 */
class WorkflowAfterCommitEventPublisherTest {

    @Test
    void shouldPublishOnlyAfterCommit() {
        List<Object> events = new ArrayList<>();
        WorkflowAfterCommitEventPublisher publisher =
                new WorkflowAfterCommitEventPublisher(events::add);
        TransactionTemplate transactionTemplate = new TransactionTemplate(new TestTransactionManager());

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(event());
            assertThat(events).isEmpty();
        });

        assertThat(events).hasSize(1);
    }

    @Test
    void shouldNotPublishAfterRollback() {
        List<Object> events = new ArrayList<>();
        WorkflowAfterCommitEventPublisher publisher =
                new WorkflowAfterCommitEventPublisher(events::add);
        TransactionTemplate transactionTemplate = new TransactionTemplate(new TestTransactionManager());

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(event());
            status.setRollbackOnly();
        });

        assertThat(events).isEmpty();
    }

    @Test
    void listenerFailureShouldBeSwallowed() {
        WorkflowAfterCommitEventPublisher publisher =
                new WorkflowAfterCommitEventPublisher(event -> {
                    throw new IllegalStateException("listener failed");
                });

        assertThatCode(() -> publisher.publish(event())).doesNotThrowAnyException();
    }

    private WorkflowChangedEvent event() {
        return new WorkflowChangedEvent(
                UUID.randomUUID(), Instant.now(), WorkflowOperation.START,
                "TEST_FLOW", 1L, 2L, "TEST:1", 3L, 4L,
                null, "submit", "0"
        );
    }

    private static final class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
