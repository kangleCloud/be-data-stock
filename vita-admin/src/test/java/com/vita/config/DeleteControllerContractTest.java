package com.vita.config;

import com.vita.controller.system.*;
import com.vita.system.sysDept.dto.SysDeptDeletedDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockDeletedDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogDeletedDto;
import com.vita.system.sysMenu.dto.SysMenuDeletedDto;
import com.vita.system.sysOperLog.dto.SysOperLogDeletedDto;
import com.vita.system.sysPermission.dto.SysPermissionBatchDeletedDto;
import com.vita.system.sysPermission.dto.SysPermissionDeletedDto;
import com.vita.system.sysRole.dto.SysRoleBatchDeletedDto;
import com.vita.system.sysRole.dto.SysRoleDeletedDto;
import com.vita.system.sysUser.dto.SysUserDeletedDto;
import com.vita.workflow.controller.WorkflowAssigneeRuleController;
import com.vita.workflow.controller.WorkflowCategoryController;
import com.vita.workflow.flow.category.dto.WorkflowCategoryDeletedDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleDeletedDto;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 删除接口 HTTP 方法和请求体契约测试。
 *
 * @author znk
 */
class DeleteControllerContractTest {

    private static final List<DeleteContract> CONTRACTS = List.of(
            new DeleteContract(SysUserController.class, "delete", SysUserDeletedDto.class, "/delete"),
            new DeleteContract(SysRoleController.class, "delete", SysRoleDeletedDto.class, "/delete"),
            new DeleteContract(SysMenuController.class, "delete", SysMenuDeletedDto.class, "/delete"),
            new DeleteContract(SysPermissionController.class, "delete", SysPermissionDeletedDto.class, "/delete"),
            new DeleteContract(SysDeptController.class, "delete", SysDeptDeletedDto.class, "/delete"),
            new DeleteContract(SysIpBlockController.class, "delete", SysIpBlockDeletedDto.class, "/delete"),
            new DeleteContract(SysOperLogController.class, "delete", SysOperLogDeletedDto.class, "/delete"),
            new DeleteContract(SysLoginLogController.class, "delete", SysLoginLogDeletedDto.class, "/delete"),
            new DeleteContract(SysRoleController.class, "deleteBatch", SysRoleBatchDeletedDto.class,
                    "/deleteBatch"),
            new DeleteContract(SysPermissionController.class, "deleteBatch",
                    SysPermissionBatchDeletedDto.class, "/deleteBatch"),
            new DeleteContract(WorkflowCategoryController.class, "delete",
                    WorkflowCategoryDeletedDto.class, "/system/workflow/categories/delete"),
            new DeleteContract(WorkflowAssigneeRuleController.class, "delete",
                    WorkflowAssigneeRuleDeletedDto.class, "/system/workflow/assignee-rules/delete"));

    @Test
    void deleteEndpointsShouldUsePostAndValidatedRequestBody() throws NoSuchMethodException {
        for (DeleteContract contract : CONTRACTS) {
            Method method = contract.controller().getDeclaredMethod(contract.method(), contract.dto());
            PostMapping postMapping = method.getDeclaredAnnotation(PostMapping.class);

            assertThat(postMapping)
                    .as(contract.controller().getSimpleName() + "#" + contract.method())
                    .isNotNull();
            assertThat(postMapping.value()).containsExactly(contract.path());
            assertThat(method.getDeclaredAnnotation(GetMapping.class)).isNull();

            Parameter parameter = method.getParameters()[0];
            assertThat(parameter.getType()).isEqualTo(contract.dto());
            assertThat(parameter.getDeclaredAnnotation(RequestBody.class)).isNotNull();
            assertThat(parameter.getDeclaredAnnotation(Valid.class)).isNotNull();
            assertThat(parameter.getDeclaredAnnotation(RequestParam.class)).isNull();
            assertThat(parameter.getDeclaredAnnotation(PathVariable.class)).isNull();
        }
    }

    private record DeleteContract(
            Class<?> controller, String method, Class<?> dto, String path) {
    }
}
