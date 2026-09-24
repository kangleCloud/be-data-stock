package com.vita.workflow.runtime.provider;

import com.vita.core.exception.ServiceException;
import com.vita.system.sysDept.entity.SysDept;
import com.vita.system.sysDept.service.ISysDeptService;
import com.vita.workflow.api.model.WorkflowStartContext;
import com.vita.workflow.constant.WorkflowConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.provider
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销流程启动上下文提供器测试
 * @Version: 1.0
 */
class ReimburseWorkflowStartContextProviderTest {

    @Test
    void shouldProvideServerSideAssigneeVariables() {
        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setLeaderUserId(200L);
        ReimburseWorkflowStartContextProvider provider =
                new ReimburseWorkflowStartContextProvider(deptService(dept));

        Map<String, Object> variables = provider.provide(context(100L, 10L));

        assertThat(variables)
                .containsEntry(WorkflowConstants.APPLICANT_PERMISSION_VARIABLE, "user:100")
                .containsEntry(WorkflowConstants.DEPT_LEADER_PERMISSION_VARIABLE, "user:200");
    }

    @Test
    void shouldRejectInitiatorWithoutDepartment() {
        ReimburseWorkflowStartContextProvider provider =
                new ReimburseWorkflowStartContextProvider(deptService(null));

        assertThatThrownBy(() -> provider.provide(context(100L, null)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未归属部门");
    }

    @Test
    void shouldRejectDepartmentWithoutLeader() {
        SysDept dept = new SysDept();
        dept.setId(10L);
        ReimburseWorkflowStartContextProvider provider =
                new ReimburseWorkflowStartContextProvider(deptService(dept));

        assertThatThrownBy(() -> provider.provide(context(100L, 10L)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未配置负责人");
    }

    private WorkflowStartContext context(Long userId, Long deptId) {
        return new WorkflowStartContext(
                "REIMBURSE_APPROVAL_V1", "REIMBURSE:1", userId, deptId, Map.of());
    }

    private ISysDeptService deptService(SysDept dept) {
        return (ISysDeptService) Proxy.newProxyInstance(
                ISysDeptService.class.getClassLoader(),
                new Class<?>[]{ISysDeptService.class},
                (proxy, method, args) -> {
                    if ("getById".equals(method.getName())) {
                        return dept;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DeptServiceStub";
                    }
                    return null;
                }
        );
    }
}
