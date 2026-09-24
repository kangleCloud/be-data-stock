package com.vita.repeat.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.repeat.annotation
 * @Author: znk
 * @CreateTime: 2026-06-17
 * @Description: 重复提交注解，用于标记需要防止短时间内重复提交的方法
 * @Version: 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 重复提交时间窗。
     */
    long interval() default 5L;

    /**
     * 时间窗单位。
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
