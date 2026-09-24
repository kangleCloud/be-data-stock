package com.vita.workflow.flow.rule;

import com.vita.core.exception.ServiceException;
import com.vita.workflow.api.WorkflowAssigneeResolver;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleCreateDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleDeletedDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleUpdateDto;
import com.vita.workflow.flow.rule.entity.WorkflowAssigneeRule;
import com.vita.workflow.flow.rule.mapper.WorkflowAssigneeRuleMapper;
import com.vita.workflow.flow.rule.service.WorkflowAssigneeRuleService;
import com.vita.workflow.flow.rule.service.impl.WorkflowAssigneeRuleServiceImpl;
import com.vita.workflow.runtime.mapper.WorkflowRuntimeQueryMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则治理测试
 * @Version: 1.0
 */
class WorkflowAssigneeRuleServiceTest {

    /**
     * 已被流程节点引用的规则不得修改参数声明。
     */
    @Test
    void referencedRuleShouldKeepArgumentDeclaration() {
        WorkflowAssigneeRule rule = rule();
        WorkflowAssigneeRuleService service = service(rule, 1L);
        WorkflowAssigneeRuleUpdateDto updateDto = new WorkflowAssigneeRuleUpdateDto();
        updateDto.setRuleName("部门负责人");
        updateDto.setResolverCode("TEST");
        updateDto.setArgumentNames(List.of("departmentId"));
        updateDto.setStatus((byte) 1);

        assertThatThrownBy(() -> service.update(1L, updateDto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不能修改参数声明");
    }

    /**
     * 参数声明不得包含重复名称。
     */
    @Test
    void duplicateArgumentNamesShouldBeRejected() {
        WorkflowAssigneeRuleService service = service(null, 0L);
        WorkflowAssigneeRuleCreateDto createDto = new WorkflowAssigneeRuleCreateDto();
        createDto.setRuleCode("TEST_RULE");
        createDto.setRuleName("测试规则");
        createDto.setResolverCode("TEST");
        createDto.setArgumentNames(List.of("deptId", "deptId"));
        createDto.setStatus((byte) 1);

        assertThatThrownBy(() -> service.create(createDto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不能为空或重复");
    }

    @Test
    void referencedRuleShouldNotBeDeleted() {
        WorkflowAssigneeRuleService service = service(rule(), 1L);

        assertThatThrownBy(() -> service.delete(deletedDto(1L)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不能删除");
    }

    @Test
    void deleteShouldRejectMissingRequestId() {
        WorkflowAssigneeRuleService service = service(null, 0L);

        assertThatThrownBy(() -> service.delete(new WorkflowAssigneeRuleDeletedDto()))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class);
    }

    private WorkflowAssigneeRuleService service(WorkflowAssigneeRule rule, long referenceCount) {
        WorkflowAssigneeResolver resolver = new WorkflowAssigneeResolver() {
            @Override
            public String code() {
                return "TEST";
            }

            @Override
            public List<Long> resolve(Map<String, Object> arguments) {
                return List.of(1L);
            }
        };
        return new WorkflowAssigneeRuleServiceImpl(
                mapper(WorkflowAssigneeRuleMapper.class, rule, 0L),
                List.of(resolver),
                mapper(WorkflowRuntimeQueryMapper.class, null, referenceCount));
    }

    private WorkflowAssigneeRule rule() {
        WorkflowAssigneeRule rule = new WorkflowAssigneeRule();
        rule.setId(1L);
        rule.setRuleCode("DEPT_LEADER");
        rule.setRuleName("部门负责人");
        rule.setResolverCode("TEST");
        rule.setArgumentNames("deptId");
        rule.setStatus((byte) 1);
        return rule;
    }

    private WorkflowAssigneeRuleDeletedDto deletedDto(Long id) {
        WorkflowAssigneeRuleDeletedDto deletedDto = new WorkflowAssigneeRuleDeletedDto();
        deletedDto.setId(id);
        return deletedDto;
    }

    @SuppressWarnings("unchecked")
    private <T> T mapper(Class<T> type, WorkflowAssigneeRule rule, long count) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> rule;
                    case "selectCount", "countNodesByPermissionExpression" -> count;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == boolean.class) {
            return false;
        }
        return null;
    }
}
