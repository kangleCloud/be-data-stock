package com.vita.workflow.controller;

import com.vita.workflow.flow.category.service.WorkflowCategoryManagementService;
import com.vita.workflow.flow.rule.service.WorkflowAssigneeRuleService;
import com.vita.workflow.reimbursement.service.WorkflowReimbursementService;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.service.IWorkflowDefinitionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流具体 Controller 注册和总开关测试
 * @Version: 1.0
 */
class WorkflowControllerRegistrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    WorkflowDefinitionController.class,
                    WorkflowInstanceController.class,
                    WorkflowTaskController.class,
                    WorkflowCategoryController.class,
                    WorkflowAssigneeRuleController.class,
                    WorkflowReimbursementController.class,
                    WorkflowReimbursementAdminController.class,
                    ServiceTestConfiguration.class);

    /**
     * 未配置总开关时默认注册七个具体 Controller。
     */
    @Test
    void concreteControllersShouldBeEnabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WorkflowDefinitionController.class);
            assertThat(context).hasSingleBean(WorkflowInstanceController.class);
            assertThat(context).hasSingleBean(WorkflowTaskController.class);
            assertThat(context).hasSingleBean(WorkflowCategoryController.class);
            assertThat(context).hasSingleBean(WorkflowAssigneeRuleController.class);
            assertThat(context).hasSingleBean(WorkflowReimbursementController.class);
            assertThat(context).hasSingleBean(WorkflowReimbursementAdminController.class);
        });
    }

    /**
     * 关闭唯一总开关后不注册任何工作流 Controller。
     */
    @Test
    void concreteControllersShouldBeDisabledTogether() {
        contextRunner.withPropertyValues("vita.workflow.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(WorkflowDefinitionController.class);
                    assertThat(context).doesNotHaveBean(WorkflowInstanceController.class);
                    assertThat(context).doesNotHaveBean(WorkflowTaskController.class);
                    assertThat(context).doesNotHaveBean(WorkflowCategoryController.class);
                    assertThat(context).doesNotHaveBean(WorkflowAssigneeRuleController.class);
                    assertThat(context).doesNotHaveBean(WorkflowReimbursementController.class);
                    assertThat(context).doesNotHaveBean(WorkflowReimbursementAdminController.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class ServiceTestConfiguration {

        @Bean
        WorkflowRuntimeService workflowRuntimeService() {
            return serviceProxy(WorkflowRuntimeService.class);
        }

        @Bean
        IWorkflowDefinitionService workflowDefinitionService() {
            return serviceProxy(IWorkflowDefinitionService.class);
        }

        @Bean
        WorkflowCategoryManagementService workflowCategoryManagementService() {
            return serviceProxy(WorkflowCategoryManagementService.class);
        }

        @Bean
        WorkflowAssigneeRuleService workflowAssigneeRuleService() {
            return serviceProxy(WorkflowAssigneeRuleService.class);
        }

        @Bean
        WorkflowReimbursementService workflowReimbursementService() {
            return serviceProxy(WorkflowReimbursementService.class);
        }
    }

    private static <T> T serviceProxy(Class<T> serviceType) {
        return serviceType.cast(Proxy.newProxyInstance(
                serviceType.getClassLoader(),
                new Class<?>[]{serviceType},
                (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == long.class) {
                        return 0L;
                    }
                    return null;
                }));
    }
}
