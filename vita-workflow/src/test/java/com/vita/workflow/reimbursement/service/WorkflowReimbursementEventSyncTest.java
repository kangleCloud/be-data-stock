package com.vita.workflow.reimbursement.service;

import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.core.exception.ServiceException;
import com.vita.workflow.api.WorkflowPort;
import com.vita.workflow.api.event.WorkflowChangedEvent;
import com.vita.workflow.api.event.WorkflowOperation;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.reimbursement.constant.ReimbursementConstants;
import com.vita.workflow.reimbursement.dto.ReimbursementUpdateDto;
import com.vita.workflow.reimbursement.entity.WorkflowReimbursement;
import com.vita.workflow.reimbursement.enums.ReimbursementStatus;
import com.vita.workflow.reimbursement.mapper.WorkflowReimbursementMapper;
import com.vita.workflow.reimbursement.service.impl.WorkflowReimbursementServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.service
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销工作流事件状态同步测试
 * @Version: 1.0
 */
class WorkflowReimbursementEventSyncTest {

    @AfterEach
    void tearDown() {
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Test
    void rejectToApplicantShouldMarkReimbursementReturned() {
        assertEventStatus(WorkflowOperation.REJECT_TO, ReimbursementConstants.APPLICANT_NODE_CODE,
                "9", ReimbursementStatus.RETURNED);
    }

    @Test
    void finishedPassShouldMarkReimbursementApproved() {
        assertEventStatus(WorkflowOperation.PASS, ReimbursementConstants.END_NODE_CODE,
                "8", ReimbursementStatus.APPROVED);
    }

    @Test
    void cancelShouldMarkReimbursementCancelled() {
        assertEventStatus(WorkflowOperation.CANCEL, "dept_leader_approve",
                "6", ReimbursementStatus.CANCELLED);
    }

    @Test
    void terminateShouldMarkReimbursementTerminated() {
        assertEventStatus(WorkflowOperation.TERMINATE, "dept_leader_approve",
                "4", ReimbursementStatus.TERMINATED);
    }

    @Test
    void invalidateShouldMarkReimbursementInvalidated() {
        assertEventStatus(WorkflowOperation.INVALIDATE, "dept_leader_approve",
                "5", ReimbursementStatus.INVALIDATED);
    }

    @Test
    void suspendAndActivateShouldNotChangeBusinessStatus() {
        AtomicReference<WorkflowReimbursement> stored = new AtomicReference<>(reimbursement());
        WorkflowReimbursementService service = new WorkflowReimbursementServiceImpl(mapper(stored), null);

        service.syncFromEvent(event(stored.get(), WorkflowOperation.SUSPEND, "7"));
        service.syncFromEvent(event(stored.get(), WorkflowOperation.ACTIVATE, "1"));

        assertThat(stored.get().getBusinessStatus()).isEqualTo(ReimbursementStatus.APPROVING);
        assertThat(stored.get().getInstanceId()).isNull();
    }

    @Test
    void nonParticipantShouldNotReadReimbursement() {
        AtomicReference<WorkflowReimbursement> stored = new AtomicReference<>(reimbursement());
        stored.get().setApplicantId(100L);
        login(200L);
        WorkflowReimbursementService service = new WorkflowReimbursementServiceImpl(mapper(stored), null);

        assertThatThrownBy(() -> service.detail(1L, false))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("无权查看");
    }

    @Test
    void approvedReimbursementShouldBeReadOnly() {
        AtomicReference<WorkflowReimbursement> stored = new AtomicReference<>(reimbursement());
        stored.get().setApplicantId(100L);
        stored.get().setBusinessStatus(ReimbursementStatus.APPROVED);
        login(100L);
        WorkflowReimbursementService service = new WorkflowReimbursementServiceImpl(mapper(stored), null);

        assertThatThrownBy(() -> service.update(1L, new ReimbursementUpdateDto()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不允许修改或提交");
    }

    @Test
    void withdrawalShouldMarkReimbursementCancelled() {
        AtomicReference<WorkflowReimbursement> stored = new AtomicReference<>(reimbursement());
        stored.get().setApplicantId(100L);
        stored.get().setInstanceId(2L);
        login(100L);
        WorkflowInstanceSnapshot cancelled = new WorkflowInstanceSnapshot();
        cancelled.setInstanceId(2L);
        WorkflowPort workflowPort = (WorkflowPort) Proxy.newProxyInstance(
                WorkflowPort.class.getClassLoader(), new Class<?>[]{WorkflowPort.class},
                (proxy, method, args) -> "cancel".equals(method.getName()) ? cancelled : null
        );
        WorkflowReimbursementService service = new WorkflowReimbursementServiceImpl(mapper(stored), workflowPort);

        service.withdraw(1L, null);

        assertThat(stored.get().getBusinessStatus()).isEqualTo(ReimbursementStatus.CANCELLED);
    }

    private void assertEventStatus(WorkflowOperation operation,
                                   String targetNodeCode,
                                   String flowStatus,
                                   ReimbursementStatus expectedStatus) {
        AtomicReference<WorkflowReimbursement> stored = new AtomicReference<>(reimbursement());
        WorkflowReimbursementService service = new WorkflowReimbursementServiceImpl(mapper(stored), null);

        service.syncFromEvent(event(stored.get(), operation, targetNodeCode, flowStatus));

        assertThat(stored.get().getBusinessStatus()).isEqualTo(expectedStatus);
        assertThat(stored.get().getInstanceId()).isEqualTo(2L);
    }

    private WorkflowChangedEvent event(WorkflowReimbursement reimbursement,
                                       WorkflowOperation operation,
                                       String flowStatus) {
        return event(reimbursement, operation, "dept_leader_approve", flowStatus);
    }

    private WorkflowChangedEvent event(WorkflowReimbursement reimbursement,
                                       WorkflowOperation operation,
                                       String targetNodeCode,
                                       String flowStatus) {
        return new WorkflowChangedEvent(
                UUID.randomUUID(), Instant.now(), operation, ReimbursementConstants.FLOW_CODE,
                1L, 2L, reimbursement.getBusinessId(), 3L, 4L,
                "source", targetNodeCode, flowStatus
        );
    }

    private WorkflowReimbursement reimbursement() {
        WorkflowReimbursement reimbursement = new WorkflowReimbursement();
        reimbursement.setId(1L);
        reimbursement.setBusinessId(ReimbursementConstants.buildBusinessId(1L));
        reimbursement.setBusinessStatus(ReimbursementStatus.APPROVING);
        return reimbursement;
    }

    private WorkflowReimbursementMapper mapper(AtomicReference<WorkflowReimbursement> stored) {
        return (WorkflowReimbursementMapper) Proxy.newProxyInstance(
                WorkflowReimbursementMapper.class.getClassLoader(),
                new Class<?>[]{WorkflowReimbursementMapper.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) {
                        return stored.get();
                    }
                    if ("selectById".equals(method.getName())) {
                        return stored.get();
                    }
                    if ("updateById".equals(method.getName())) {
                        stored.set((WorkflowReimbursement) args[0]);
                        return 1;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    if (method.getReturnType() == long.class) {
                        return 0L;
                    }
                    return null;
                }
        );
    }

    private void login(Long userId) {
        LoginUserInfoModel loginUser = new LoginUserInfoModel();
        loginUser.setId(userId);
        LoginUserInfoModelContext.setLoginUserInfo(loginUser);
    }
}
