package com.vita.log.annotation;

import java.lang.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.annotation
 * @Author: znk
 * @CreateTime: 2026-03-12  21:41:04
 * @Description: 日志注解，用于标记需要记录日志的方法
 * @Version: 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 模块
     */
    String module() default "app";

    /**
     * 功能
     */
    String businessType() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveRequestData() default true;

    /**
     * 是否保存响应结果
     */
    boolean saveResponseData() default false;
}
