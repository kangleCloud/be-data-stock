package com.vita.async.property;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.async.property
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 默认异步线程池属性配置
 * @Version: 1.0
 */
public class DefaultAsyncExecutorProperty extends AsyncExecutorProperty {

    public DefaultAsyncExecutorProperty() {
        super(50, 200, 1000, 300, "default-async-", 30);
    }
}
