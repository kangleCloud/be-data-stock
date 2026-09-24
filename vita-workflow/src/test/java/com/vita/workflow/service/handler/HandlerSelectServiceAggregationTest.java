package com.vita.workflow.service.handler;

import com.vita.system.sysDept.service.ISysDeptService;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysUser.service.ISysUserService;
import com.vita.workflow.api.WorkflowHandlerSource;
import com.vita.workflow.constant.WorkflowConstants;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.vo.HandlerSelectVo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.service.handler
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流办理人来源聚合测试
 * @Version: 1.0
 */
class HandlerSelectServiceAggregationTest {

    /**
     * 自定义来源应与内置来源聚合并接收标准查询参数。
     */
    @Test
    void customSourceShouldBeAggregated() {
        AtomicReference<Map<String, Object>> receivedQuery = new AtomicReference<>();
        HandlerSelectVo expected = new HandlerSelectVo();
        WorkflowHandlerSource source = source("POST", query -> {
            receivedQuery.set(query);
            return expected;
        });
        HandlerSelectServiceImpl service = service(List.of(source));
        HandlerQuery query = new HandlerQuery();
        query.setHandlerType("POST");
        query.setHandlerCode("DEV");
        query.setPageNum(2);
        query.setPageSize(10);

        HandlerSelectVo actual = service.getHandlerSelect(query);

        assertThat(service.getHandlerType()).containsExactly(
                WorkflowConstants.USER_HANDLER_TYPE,
                WorkflowConstants.ROLE_HANDLER_TYPE,
                WorkflowConstants.DEPT_HANDLER_TYPE,
                WorkflowConstants.RULE_HANDLER_TYPE,
                "POST");
        assertThat(actual).isSameAs(expected);
        assertThat(receivedQuery.get())
                .containsEntry("handlerType", "POST")
                .containsEntry("handlerCode", "DEV")
                .containsEntry("pageNum", 2)
                .containsEntry("pageSize", 10);
    }

    /**
     * 扩展来源不得覆盖平台内置来源。
     */
    @Test
    void duplicateSourceTypeShouldBeRejected() {
        WorkflowHandlerSource source = source(
                WorkflowConstants.USER_HANDLER_TYPE, query -> new HandlerSelectVo());

        assertThatThrownBy(() -> service(List.of(source)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("办理人来源编码重复");
    }

    private HandlerSelectServiceImpl service(List<WorkflowHandlerSource> sources) {
        return new HandlerSelectServiceImpl(
                proxy(ISysRoleService.class),
                proxy(ISysDeptService.class),
                proxy(ISysUserService.class),
                sources);
    }

    private WorkflowHandlerSource source(
            String type,
            java.util.function.Function<Map<String, Object>, Object> queryFunction) {
        return new WorkflowHandlerSource() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public Object query(Map<String, Object> query) {
                return queryFunction.apply(query);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> null);
    }
}
