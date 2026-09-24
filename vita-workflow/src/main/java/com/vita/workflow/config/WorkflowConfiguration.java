package com.vita.workflow.config;

import com.vita.workflow.handler.WorkflowPermissionHandler;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.service.IWorkflowDefinitionService;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.ui.service.CategoryService;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.config
 * @Author: Codex
 * @CreateTime: 2026-07-02
 * @Description: Warm-Flow 一期配置
 * @Version: 1.0
 */
@Configuration
@ConditionalOnWorkflowEnabled
public class WorkflowConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowConfiguration.class);

    /**
     * 注册工作流启动自检任务。
     *
     * @param applicationContext Spring 应用上下文
     * @return 启动自检任务
     */
    @Bean
    public ApplicationRunner workflowStartupVerifier(ApplicationContext applicationContext) {
        return args -> verifyWorkflowComponents(applicationContext);
    }

    private void verifyWorkflowComponents(ApplicationContext applicationContext) {
        if (FlowEngine.getFlowConfig() == null) {
            throw new IllegalStateException("Warm-Flow 启动自检失败，流程引擎未初始化");
        }

        WorkflowPermissionHandler permissionHandler = requireBean(applicationContext,
                WorkflowPermissionHandler.class);
        PermissionHandler enginePermissionHandler = FlowEngine.permissionHandler();
        if (enginePermissionHandler != permissionHandler) {
            throw new IllegalStateException("Warm-Flow 启动自检失败，WorkflowPermissionHandler 未绑定到流程引擎");
        }

        requireBean(applicationContext, HandlerSelectService.class);
        requireBean(applicationContext, CategoryService.class);
        requireBean(applicationContext, IWorkflowDefinitionService.class);
        requireBean(applicationContext, WorkflowRuntimeService.class);
        LOGGER.info("Warm-Flow 启动自检通过");
    }

    private <T> T requireBean(ApplicationContext applicationContext, Class<T> beanType) {
        try {
            return applicationContext.getBean(beanType);
        } catch (BeansException ex) {
            throw new IllegalStateException("Warm-Flow 启动自检失败，缺少组件：" + beanType.getName(), ex);
        }
    }
}
