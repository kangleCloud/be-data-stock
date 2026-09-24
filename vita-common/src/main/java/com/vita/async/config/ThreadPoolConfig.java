package com.vita.async.config;

import com.vita.async.property.AsyncExecutorProperty;
import com.vita.async.property.DefaultAsyncExecutorProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.async.config
 * @Author: znk
 * @CreateTime: 2026-03-13  17:20:25
 * @Description: 线程池配置类
 * @Version: 1.0
 */
@Configuration
public class ThreadPoolConfig {
    private static final DefaultAsyncExecutorProperty DEFAULT_EXECUTOR_PROPERTY = new DefaultAsyncExecutorProperty();

    /**
     * 非核心异步默认线程池，同时保留旧别名避免 Bean 名立即断裂。
     *
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = {"defaultTaskExecutor", "threadPoolTaskExecutor"})
    @Primary
    public ThreadPoolTaskExecutor defaultTaskExecutor() {
        return createExecutor(DEFAULT_EXECUTOR_PROPERTY, new ThreadPoolExecutor.DiscardPolicy());
    }

    /**
     * 创建线程池执行器实例。
     *
     * @param property 线程池配置对象
     * @param rejectedExecutionHandler 拒绝策略处理器，当线程池无法接受新任务时执行
     * @return 配置完成的 ThreadPoolTaskExecutor 实例
     */
    private ThreadPoolTaskExecutor createExecutor(AsyncExecutorProperty property,
                                                  RejectedExecutionHandler rejectedExecutionHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(property.getCorePoolSize());
        executor.setMaxPoolSize(property.getMaxPoolSize());
        executor.setQueueCapacity(property.getQueueCapacity());
        executor.setKeepAliveSeconds(property.getKeepAliveSeconds());
        executor.setThreadNamePrefix(property.getThreadNamePrefix());
        // 配置优雅关闭：等待任务完成并设置终止超时时间
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(property.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
        return executor;
    }
}
