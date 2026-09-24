package com.vita.workflow.runtime.support;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.support
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流分布式互斥操作支持
 * @Version: 1.0
 */
@Component
@ConditionalOnWorkflowEnabled
public class WorkflowDistributedLock {

    private static final long LEASE_SECONDS = 60L;

    private final RedissonClient redissonClient;

    public WorkflowDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 在分布式锁内执行操作。
     *
     * @param key 锁键
     * @param action 操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T execute(String key, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException(GlobalErrorCode.LOCKED);
            }
            return action.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(GlobalErrorCode.LOCKED);
        } finally {
            if (locked && !releaseAfterTransaction(lock)) {
                release(lock);
            }
        }
    }

    private boolean releaseAfterTransaction(RLock lock) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        // 外层事务提交前必须继续持锁，避免数据库尚未提交时相同操作再次进入。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                release(lock);
            }
        });
        return true;
    }

    private void release(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
