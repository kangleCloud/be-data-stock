package com.vita.workflow.runtime.service;

import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.core.exception.ServiceException;
import com.vita.workflow.api.command.WorkflowCancelCommand;
import com.vita.workflow.api.command.WorkflowRejectCommand;
import com.vita.workflow.api.command.WorkflowStartCommand;
import com.vita.workflow.api.command.WorkflowTaskCommand;
import com.vita.workflow.runtime.mapper.WorkflowRuntimeQueryMapper;
import com.vita.workflow.runtime.model.WorkflowNodePolicy;
import com.vita.workflow.runtime.service.impl.WorkflowRuntimeServiceImpl;
import com.vita.workflow.runtime.support.WorkflowAfterCommitEventPublisher;
import com.vita.workflow.runtime.support.WorkflowDistributedLock;
import com.vita.workflow.runtime.vo.WorkflowHistoryVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceVo;
import com.vita.workflow.runtime.vo.WorkflowTaskVo;
import com.vita.workflow.support.WorkflowLoginUserSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.service
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流运行时关键校验分支测试
 * @Version: 1.0
 */
class WorkflowRuntimeServiceValidationTest {

    @BeforeEach
    void setUp() {
        LoginUserInfoModel loginUser = new LoginUserInfoModel();
        loginUser.setId(100L);
        loginUser.setDeptId(10L);
        LoginUserInfoModelContext.setLoginUserInfo(loginUser);
    }

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Test
    void duplicateBusinessIdShouldBeRejected() {
        WorkflowInstanceVo existing = new WorkflowInstanceVo();
        existing.setInstanceId(1L);
        WorkflowRuntimeService service = service(Map.of("selectInstanceByBusinessId", existing));
        WorkflowStartCommand command = new WorkflowStartCommand();
        command.setFlowCode("TEST_FLOW");
        command.setBusinessId("TEST:1");

        assertThatThrownBy(() -> service.start(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("业务ID已存在");
    }

    @Test
    void completedTaskShouldBeReportedAsMissing() {
        WorkflowRuntimeService service = service(Map.of());
        WorkflowTaskCommand command = new WorkflowTaskCommand();
        command.setTaskId(1L);

        assertThatThrownBy(() -> service.pass(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在或已办理");
    }

    @Test
    void invalidRejectTargetShouldBeRejected() {
        WorkflowTaskVo task = new WorkflowTaskVo();
        task.setTaskId(1L);
        task.setInstanceId(2L);
        task.setAllowedRejectNodeCode("submit_apply");
        Map<String, Object> responses = new HashMap<>();
        responses.put("selectTaskById", task);
        responses.put("countTaskAccess", 1L);
        WorkflowRuntimeService service = service(responses);
        WorkflowRejectCommand command = new WorkflowRejectCommand();
        command.setTaskId(1L);
        command.setTargetNodeCode("illegal_node");

        assertThatThrownBy(() -> service.rejectTo(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不在当前流程定义允许的退回范围");
    }

    @Test
    void nonInitiatorShouldNotCancelInstance() {
        WorkflowInstanceVo instance = new WorkflowInstanceVo();
        instance.setInstanceId(2L);
        instance.setInitiatorId("999");
        instance.setNodeType(1);
        WorkflowRuntimeService service = service(Map.of("selectInstanceById", instance));
        WorkflowCancelCommand command = new WorkflowCancelCommand();
        command.setInstanceId(2L);

        assertThatThrownBy(() -> service.cancel(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("只有流程发起人");
    }

    @Test
    void instanceWithApproverHistoryShouldNotBeWithdrawn() {
        WorkflowInstanceVo instance = new WorkflowInstanceVo();
        instance.setInstanceId(2L);
        instance.setInitiatorId("100");
        instance.setNodeType(1);

        WorkflowHistoryVo applicantHistory = new WorkflowHistoryVo();
        applicantHistory.setNodeType(1);
        applicantHistory.setApproverId("100");
        WorkflowHistoryVo approverHistory = new WorkflowHistoryVo();
        approverHistory.setNodeType(1);
        approverHistory.setApproverId("200");

        Map<String, Object> responses = new HashMap<>();
        responses.put("selectInstanceById", instance);
        responses.put("selectHistoryByInstanceId", List.of(applicantHistory, approverHistory));
        WorkflowRuntimeService service = service(responses);
        WorkflowCancelCommand command = new WorkflowCancelCommand();
        command.setInstanceId(2L);

        assertThatThrownBy(() -> service.cancel(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已有审批人办理");
    }

    /**
     * 平台内部变量在查询白名单之前即被拒绝。
     */
    @Test
    void protectedVariableShouldNotBeUpdated() {
        WorkflowRuntimeService service = service(Map.of());

        assertThatThrownBy(() -> service.updateInstanceVariable(
                1L, "_vitaNextAssignee:approve", 200L, "测试"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("平台保护变量");
    }

    /**
     * 未注册变量策略的流程保持只读。
     */
    @Test
    void flowWithoutVariablePolicyShouldBeReadOnly() {
        WorkflowInstanceVo instance = new WorkflowInstanceVo();
        instance.setInstanceId(1L);
        instance.setFlowCode("READ_ONLY_FLOW");
        instance.setNodeType(1);
        WorkflowRuntimeService service = service(Map.of("selectInstanceById", instance));

        assertThatThrownBy(() -> service.updateInstanceVariable(
                1L, "amount", 100, "修正金额"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未开放该变量");
    }

    /**
     * 实例变量更新必须先校验实例 ID，禁止生成空 ID 锁键。
     */
    @Test
    void variableUpdateShouldRequireInstanceId() {
        WorkflowRuntimeService service = service(Map.of());

        assertThatThrownBy(() -> service.updateInstanceVariable(
                null, "amount", 100, "修正金额"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("实例ID不能为空");
    }

    private WorkflowRuntimeService service(Map<String, Object> responses) {
        return new WorkflowRuntimeServiceImpl(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                mapper(responses),
                null,
                null,
                null,
                (definitionId, nodeCode) -> WorkflowNodePolicy.legacy(),
                null,
                new WorkflowLoginUserSupport(),
                java.util.List.of(),
                java.util.List.of(),
                new WorkflowDistributedLock(redissonClient(true)),
                new WorkflowAfterCommitEventPublisher(event -> {
                }),
                new TestTransactionManager()
        );
    }

    private WorkflowRuntimeQueryMapper mapper(Map<String, Object> responses) {
        return (WorkflowRuntimeQueryMapper) Proxy.newProxyInstance(
                WorkflowRuntimeQueryMapper.class.getClassLoader(),
                new Class<?>[]{WorkflowRuntimeQueryMapper.class},
                (proxy, method, args) -> {
                    if (responses.containsKey(method.getName())) {
                        return responses.get(method.getName());
                    }
                    if (method.getReturnType() == long.class) {
                        return 0L;
                    }
                    return null;
                }
        );
    }

    private RedissonClient redissonClient(boolean lockAcquired) {
        RLock lock = (RLock) Proxy.newProxyInstance(
                RLock.class.getClassLoader(),
                new Class<?>[]{RLock.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "tryLock", "isHeldByCurrentThread" -> lockAcquired;
                    default -> null;
                }
        );
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(),
                new Class<?>[]{RedissonClient.class},
                (proxy, method, args) -> "getLock".equals(method.getName()) ? lock : null
        );
    }

    private static final class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
