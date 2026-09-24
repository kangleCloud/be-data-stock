package com.vita.workflow.runtime.support;

import com.vita.core.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.support
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流分布式锁测试
 * @Version: 1.0
 */
class WorkflowDistributedLockTest {

    @Test
    void lockCompetitionShouldReturn423() {
        WorkflowDistributedLock lock = new WorkflowDistributedLock(redissonClient(false, new AtomicBoolean()));

        assertThatThrownBy(() -> lock.execute("test", () -> true))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(423);
    }

    @Test
    void lockShouldBeReleasedWhenActionFails() {
        AtomicBoolean unlocked = new AtomicBoolean();
        WorkflowDistributedLock lock = new WorkflowDistributedLock(redissonClient(true, unlocked));

        assertThatThrownBy(() -> lock.execute("test", () -> {
            throw new IllegalStateException("failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(unlocked).isTrue();
    }

    @Test
    void lockShouldBeHeldUntilOuterTransactionCompletes() {
        AtomicBoolean unlocked = new AtomicBoolean();
        WorkflowDistributedLock lock = new WorkflowDistributedLock(redissonClient(true, unlocked));
        TransactionTemplate transactionTemplate = new TransactionTemplate(new TestTransactionManager());

        transactionTemplate.executeWithoutResult(status -> {
            lock.execute("test", () -> true);
            assertThat(unlocked).isFalse();
        });

        assertThat(unlocked).isTrue();
    }

    private RedissonClient redissonClient(boolean acquired, AtomicBoolean unlocked) {
        RLock lock = (RLock) Proxy.newProxyInstance(
                RLock.class.getClassLoader(),
                new Class<?>[]{RLock.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "tryLock", "isHeldByCurrentThread" -> acquired;
                    case "unlock" -> {
                        unlocked.set(true);
                        yield null;
                    }
                    default -> null;
                }
        );
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(),
                new Class<?>[]{RedissonClient.class},
                (proxy, method, args) -> "getLock".equals(method.getName()) ? lock : null
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
