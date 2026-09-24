package com.vita.async.property;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.async.property
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 异步线程池属性模型
 * @Version: 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class AsyncExecutorProperty {
    /**
     * 核心线程数。
     */
    private int corePoolSize;

    /**
     * 最大线程数。
     */
    private int maxPoolSize;

    /**
     * 队列容量。
     */
    private int queueCapacity;

    /**
     * 空闲线程存活时间，单位秒。
     */
    private int keepAliveSeconds;

    /**
     * 线程名前缀。
     */
    private String threadNamePrefix;

    /**
     * 关闭时等待任务完成的最长秒数。
     */
    private int awaitTerminationSeconds;

    public AsyncExecutorProperty(int corePoolSize,
                                 int maxPoolSize,
                                 int queueCapacity,
                                 int keepAliveSeconds,
                                 String threadNamePrefix,
                                 int awaitTerminationSeconds) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueCapacity = queueCapacity;
        this.keepAliveSeconds = keepAliveSeconds;
        this.threadNamePrefix = threadNamePrefix;
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }
}
