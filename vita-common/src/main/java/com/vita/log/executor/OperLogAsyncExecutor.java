package com.vita.log.executor;

import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.log.model.OperLogRecord;
import com.vita.log.persistence.OperLogPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.executor
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 操作日志异步执行器
 * @Version: 1.0
 */
@Component
public class OperLogAsyncExecutor {

    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("sys-access");

    private static final Logger LOG = LoggerFactory.getLogger(OperLogAsyncExecutor.class);

    private final Executor defaultTaskExecutor;

    private final ObjectProvider<OperLogPersistence> operLogPersistenceProvider;

    public OperLogAsyncExecutor(@Qualifier("defaultTaskExecutor") Executor defaultTaskExecutor,
                                ObjectProvider<OperLogPersistence> operLogPersistenceProvider) {
        this.defaultTaskExecutor = defaultTaskExecutor;
        this.operLogPersistenceProvider = operLogPersistenceProvider;
    }

    /**
     * 提交系统操作日志任务。
     *
     * @param requestId 请求ID
     * @param record    操作日志快照
     */
    public void submit(String requestId, OperLogRecord record) {
        if (record == null) {
            return;
        }

        OperLogPersistence operLogPersistence = operLogPersistenceProvider.getIfAvailable();
        OperLogRecord snapshot = snapshot(record);
        defaultTaskExecutor.execute(() -> {
            try {
                withRequestId(requestId);
                ACCESS_LOGGER.info(
                        "requestId={} module={} businessType={} status={} requestMethod={} operUrl={} operIp={} operUserId={} operName={} costTime={} errorMsg={}",
                        requestId,
                        snapshot.getModule(),
                        snapshot.getBusinessType(),
                        snapshot.getStatus(),
                        snapshot.getRequestMethod(),
                        snapshot.getOperUrl(),
                        snapshot.getOperIp(),
                        snapshot.getOperUserId(),
                        snapshot.getOperName(),
                        snapshot.getCostTime(),
                        snapshot.getErrorMsg()
                );
                if (operLogPersistence != null) {
                    operLogPersistence.persist(snapshot);
                }
            } catch (Exception ex) {
                LOG.error("异步保存系统操作日志失败, requestId={}", requestId, ex);
            } finally {
                MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
            }
        });
    }

    /**
     * 创建操作日志记录的快照，避免异步执行过程中数据被修改。
     *
     * @param source 日志记录源对象
     * @return 日志记录快照
     */
    private OperLogRecord snapshot(OperLogRecord source) {
        OperLogRecord target = new OperLogRecord();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private void withRequestId(String requestId) {
        if (requestId != null) {
            MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
        }
    }
}
