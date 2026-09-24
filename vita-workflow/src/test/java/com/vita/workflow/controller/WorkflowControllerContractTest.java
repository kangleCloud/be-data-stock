package com.vita.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.dto.WorkflowTaskHandlersUpdateRequest;
import com.vita.workflow.controller.dto.WorkflowVariableUpdateRequest;
import org.dromara.warm.flow.core.dto.DefJson;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流具体 Controller 路由、权限和 AOP 契约测试
 * @Version: 1.0
 */
class WorkflowControllerContractTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
            WorkflowDefinitionController.class,
            WorkflowInstanceController.class,
            WorkflowTaskController.class,
            WorkflowCategoryController.class,
            WorkflowAssigneeRuleController.class,
            WorkflowReimbursementController.class,
            WorkflowReimbursementAdminController.class);

    /**
     * 验证七个 Controller 的路由唯一，并保留旧路由及新增治理路由。
     */
    @Test
    void concreteControllersShouldExposeUniqueCompatibleRoutes() {
        List<String> routes = new ArrayList<>();
        CONTROLLERS.forEach(controller -> collectRoutes(controller, routes));

        assertThat(routes).hasSize(70);
        assertThat(new LinkedHashSet<>(routes)).hasSameSizeAs(routes);
        assertThat(routes).contains(
                "GET /workflow/definitions/published",
                "POST /workflow/instances/start",
                "GET /workflow/instances/mine",
                "GET /workflow/instances/{instanceId}",
                "GET /workflow/instances/{instanceId}/history",
                "POST /workflow/instances/{instanceId}/cancel",
                "GET /workflow/tasks/pending",
                "GET /workflow/tasks/completed",
                "GET /workflow/tasks/{taskId}",
                "POST /workflow/tasks/{taskId}/pass",
                "POST /workflow/tasks/{taskId}/reject-last",
                "POST /workflow/tasks/{taskId}/reject-to",
                "POST /workflow/reimbursements",
                "POST /workflow/reimbursements/{id}",
                "GET /workflow/reimbursements/mine",
                "POST /workflow/reimbursements/{id}/submit",
                "POST /workflow/reimbursements/{id}/withdraw",
                "GET /system/workflow/definitions/page",
                "POST /system/workflow/importJson",
                "GET /system/workflow/instances/running",
                "GET /system/workflow/instances/finished",
                "GET /system/workflow/tasks/pending",
                "GET /system/workflow/tasks/completed",
                "GET /system/workflow/categories/tree",
                "POST /system/workflow/categories/delete",
                "GET /system/workflow/assignee-rules",
                "POST /system/workflow/assignee-rules/delete",
                "GET /system/workflow/reimbursements/page",
                "POST /system/workflow/reimbursements/resync",
                "GET /workflow/instances/by-business/{businessId}",
                "GET /workflow/instances/by-business/{businessId}/history",
                "GET /workflow/instances/by-business/{businessId}/graph",
                "GET /system/workflow/instances/by-business/{businessId}",
                "GET /system/workflow/instances/by-business/{businessId}/history",
                "GET /system/workflow/instances/by-business/{businessId}/graph",
                "GET /system/workflow/instances/{instanceId}/variables",
                "POST /system/workflow/instances/{instanceId}/variables",
                "POST /system/workflow/definitions/{definitionId}",
                "POST /system/workflow/instances/{instanceId}",
                "POST /system/workflow/instances/by-business/{businessId}",
                "POST /system/workflow/instances/{instanceId}/history",
                "GET /workflow/tasks/{taskId}/handlers",
                "POST /system/workflow/tasks/{taskId}/handlers");
    }

    /**
     * 验证每个系统接口都在具体实现方法上直接声明权限码。
     */
    @Test
    void everyManagementRouteShouldDeclarePermissionDirectly() {
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                Set<String> routes = new LinkedHashSet<>();
                collectRoutes(controller, method, routes);
                if (routes.stream().noneMatch(route -> route.contains(" /system/workflow/"))) {
                    continue;
                }
                assertThat(method.getDeclaredAnnotation(SaCheckPermission.class))
                        .as(controller.getSimpleName() + "#" + method.getName())
                        .isNotNull();
            }
        }
    }

    /**
     * 验证新增实例变量和任务办理人接口使用预定权限码。
     *
     * @throws NoSuchMethodException 方法不存在
     */
    @Test
    void governanceWritesShouldUseManagePermissions() throws NoSuchMethodException {
        assertPermission(WorkflowInstanceController.class, "updateInstanceVariable",
                WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION,
                Long.class, WorkflowVariableUpdateRequest.class);
        assertPermission(WorkflowInstanceController.class, "deleteRunningInstance",
                WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION, Long.class);
        assertPermission(WorkflowInstanceController.class, "deleteRunningInstanceByBusinessId",
                WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION, String.class);
        assertPermission(WorkflowInstanceController.class, "deleteFinishedInstanceHistory",
                WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION, Long.class);
        assertPermission(WorkflowTaskController.class, "updateHandlers",
                WorkflowConstants.WORKFLOW_TASK_MANAGE_PERMISSION,
                Long.class, WorkflowTaskHandlersUpdateRequest.class);

        Method importMethod = WorkflowDefinitionController.class.getMethod(
                "importJson", DefJson.class);
        assertThat(importMethod.getAnnotation(RepeatSubmit.class)).isNotNull();
    }

    /**
     * 除下一节点预览外，所有 POST 接口都必须记录日志并防重复提交。
     */
    @Test
    void stateChangingRoutesShouldDeclareLogAndRepeatProtection() {
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping =
                        AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null || "nextNodes".equals(method.getName())
                        || Arrays.stream(mapping.method()).allMatch(
                        requestMethod -> requestMethod == RequestMethod.GET)) {
                    continue;
                }
                assertThat(method.getDeclaredAnnotation(Log.class))
                        .as(controller.getSimpleName() + "#" + method.getName())
                        .isNotNull();
                assertThat(method.getDeclaredAnnotation(RepeatSubmit.class))
                        .as(controller.getSimpleName() + "#" + method.getName())
                        .isNotNull();
            }
        }
    }

    /**
     * 验证 workflow 模块具体 Controller 仍命中重复提交切点。
     *
     * @throws NoSuchMethodException 方法不存在
     */
    @Test
    void workflowControllerShouldMatchRepeatSubmitPointcut() throws NoSuchMethodException {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public * com.vita..controller..*Controller.*(..))");
        Method method = WorkflowDefinitionController.class.getMethod("importJson", DefJson.class);

        assertThat(pointcut.matches(method, WorkflowDefinitionController.class)).isTrue();
    }

    private void assertPermission(Class<?> controllerType, String methodName, String permission,
                                  Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = controllerType.getMethod(methodName, parameterTypes);
        SaCheckPermission annotation = method.getDeclaredAnnotation(SaCheckPermission.class);
        assertThat(annotation).as(controllerType.getSimpleName() + "#" + methodName).isNotNull();
        assertThat(annotation.value()).containsExactly(permission);
    }

    private void collectRoutes(Class<?> controllerType, List<String> routes) {
        Arrays.stream(controllerType.getDeclaredMethods())
                .forEach(method -> collectRoutes(controllerType, method, routes));
    }

    private void collectRoutes(Class<?> controllerType, Method method, java.util.Collection<String> routes) {
        RequestMapping methodMapping =
                AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (methodMapping == null) {
            return;
        }
        RequestMapping classMapping =
                AnnotatedElementUtils.findMergedAnnotation(controllerType, RequestMapping.class);
        String prefix = classMapping == null || classMapping.value().length == 0
                ? "" : classMapping.value()[0];
        String[] paths = methodMapping.value().length == 0
                ? new String[]{""} : methodMapping.value();
        for (RequestMethod requestMethod : methodMapping.method()) {
            Arrays.stream(paths)
                    .map(path -> requestMethod.name() + " " + prefix + path)
                    .forEach(routes::add);
        }
    }
}
