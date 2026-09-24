package com.vita.workflow.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.config
 * @Author: znk
 * @CreateTime: 2026-07-14
 * @Description: 工作流能力启用条件
 * @Version: 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "vita.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnWorkflowEnabled {
}
