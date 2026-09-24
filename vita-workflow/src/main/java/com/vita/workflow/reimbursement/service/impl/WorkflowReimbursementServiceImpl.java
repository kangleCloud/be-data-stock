package com.vita.workflow.reimbursement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.utils.id.SnowUtils;
import com.vita.workflow.api.WorkflowPort;
import com.vita.workflow.api.command.WorkflowCancelCommand;
import com.vita.workflow.api.command.WorkflowSubmitCommand;
import com.vita.workflow.api.command.WorkflowTaskCommand;
import com.vita.workflow.api.event.WorkflowChangedEvent;
import com.vita.workflow.api.event.WorkflowOperation;
import com.vita.workflow.api.model.WorkflowBusinessMetadata;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.reimbursement.constant.ReimbursementConstants;
import com.vita.workflow.reimbursement.dto.*;
import com.vita.workflow.reimbursement.entity.WorkflowReimbursement;
import com.vita.workflow.reimbursement.enums.ReimbursementStatus;
import com.vita.workflow.reimbursement.mapper.WorkflowReimbursementMapper;
import com.vita.workflow.reimbursement.service.WorkflowReimbursementService;
import com.vita.workflow.reimbursement.vo.ReimbursementDetailVo;
import com.vita.workflow.reimbursement.vo.ReimbursementPageVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.service.impl
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销工作流业务服务实现
 * @Version: 1.0
 */
@Service
@ConditionalOnWorkflowEnabled
public class WorkflowReimbursementServiceImpl implements WorkflowReimbursementService {

    private static final String FLOW_STATUS_TOBE_SUBMIT = "0";
    private static final String FLOW_STATUS_PASS = "2";
    private static final String FLOW_STATUS_AUTO_PASS = "3";
    private static final String FLOW_STATUS_TERMINATE = "4";
    private static final String FLOW_STATUS_NULLIFY = "5";
    private static final String FLOW_STATUS_CANCEL = "6";
    private static final String FLOW_STATUS_FINISHED = "8";
    private static final String FLOW_STATUS_INVALID = "10";

    private final WorkflowReimbursementMapper reimbursementMapper;
    private final WorkflowPort workflowPort;

    public WorkflowReimbursementServiceImpl(WorkflowReimbursementMapper reimbursementMapper,
                                            WorkflowPort workflowPort) {
        this.reimbursementMapper = reimbursementMapper;
        this.workflowPort = workflowPort;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReimbursementCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        LoginUserInfoModel loginUser = LoginUserInfoModelContext.getRequiredLoginUserInfo();
        WorkflowReimbursement reimbursement = new WorkflowReimbursement();
        BeanUtils.copyProperties(createDto, reimbursement);
        reimbursement.setId(SnowUtils.getSnowflakeNextId());
        reimbursement.setBusinessId(ReimbursementConstants.buildBusinessId(reimbursement.getId()));
        reimbursement.setApplicantId(loginUser.getId());
        reimbursement.setApplicantDeptId(loginUser.getDeptId());
        reimbursement.setBusinessStatus(ReimbursementStatus.DRAFT);
        reimbursement.setVersion(0);
        reimbursementMapper.insert(reimbursement);
        return reimbursement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ReimbursementUpdateDto updateDto) {
        if (updateDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        WorkflowReimbursement reimbursement = requireOwned(id);
        requireEditable(reimbursement);
        BeanUtils.copyProperties(updateDto, reimbursement);
        reimbursementMapper.updateById(reimbursement);
    }

    @Override
    public PageResponse<ReimbursementPageVo> mine(ReimbursementSearchDto searchDto) {
        return pageInternal(searchDto, LoginUserInfoModelContext.getRequiredLoginUserId());
    }

    @Override
    public PageResponse<ReimbursementPageVo> page(ReimbursementSearchDto searchDto) {
        Long applicantId = searchDto == null ? null : searchDto.getApplicantId();
        return pageInternal(searchDto, applicantId);
    }

    @Override
    public ReimbursementDetailVo detail(Long id, boolean administrator) {
        WorkflowReimbursement reimbursement = requireEntity(id);
        Long userId = LoginUserInfoModelContext.getRequiredLoginUserId();
        if (!administrator && !userId.equals(reimbursement.getApplicantId())
                && (reimbursement.getInstanceId() == null
                || !workflowPort.canAccess(reimbursement.getBusinessId(), userId))) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "无权查看该报销单");
        }
        return toDetailVo(reimbursement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementDetailVo submit(Long id, ReimbursementSubmitDto submitDto) {
        ReimbursementSubmitDto commandDto = submitDto == null ? new ReimbursementSubmitDto() : submitDto;
        WorkflowReimbursement reimbursement = requireOwned(id);
        requireEditable(reimbursement);

        WorkflowInstanceSnapshot snapshot;
        if (reimbursement.getBusinessStatus() == ReimbursementStatus.DRAFT) {
            // 首次提交原子创建实例并办理申请人首任务，避免业务单与流程实例状态分离。
            WorkflowSubmitCommand command = new WorkflowSubmitCommand();
            command.setFlowCode(ReimbursementConstants.FLOW_CODE);
            command.setBusinessId(reimbursement.getBusinessId());
            command.setMessage(commandDto.getMessage());
            command.setVariables(commandDto.getVariables());
            WorkflowBusinessMetadata metadata = new WorkflowBusinessMetadata();
            metadata.setBusinessCode(ReimbursementConstants.BUSINESS_CODE);
            metadata.setBusinessTitle(reimbursement.getTitle());
            command.setMetadata(metadata);
            snapshot = workflowPort.startAndPass(command);
            reimbursement.setInstanceId(snapshot.getInstanceId());
        } else {
            // 退回申请节点后只办理现有任务，businessId 不得再次创建流程实例。
            WorkflowInstanceSnapshot current = workflowPort.getByBusinessId(reimbursement.getBusinessId());
            if (current.getTaskId() == null
                    || !ReimbursementConstants.APPLICANT_NODE_CODE.equals(current.getNodeCode())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "报销单当前不在申请人提交节点");
            }
            WorkflowTaskCommand command = new WorkflowTaskCommand();
            command.setTaskId(current.getTaskId());
            command.setMessage(commandDto.getMessage());
            command.setVariables(commandDto.getVariables());
            snapshot = workflowPort.pass(command);
        }

        reimbursement.setInstanceId(snapshot.getInstanceId());
        ReimbursementStatus submittedStatus = resolveStatus(snapshot);
        reimbursement.setBusinessStatus(submittedStatus == ReimbursementStatus.APPROVED
                ? ReimbursementStatus.APPROVED : ReimbursementStatus.APPROVING);
        reimbursementMapper.updateById(reimbursement);
        return toDetailVo(reimbursement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementDetailVo withdraw(Long id, ReimbursementWithdrawDto withdrawDto) {
        WorkflowReimbursement reimbursement = requireOwned(id);
        if (reimbursement.getBusinessStatus() != ReimbursementStatus.APPROVING
                || reimbursement.getInstanceId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "当前报销单不能撤回");
        }
        WorkflowCancelCommand command = new WorkflowCancelCommand();
        command.setInstanceId(reimbursement.getInstanceId());
        command.setMessage(withdrawDto == null ? null : withdrawDto.getMessage());
        workflowPort.cancel(command);
        reimbursement.setBusinessStatus(ReimbursementStatus.CANCELLED);
        reimbursementMapper.updateById(reimbursement);
        return toDetailVo(reimbursement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementDetailVo resync(String businessId) {
        if (!StringUtils.hasText(businessId)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务ID不能为空");
        }
        WorkflowReimbursement reimbursement = requireByBusinessId(businessId);
        WorkflowInstanceSnapshot snapshot = workflowPort.getByBusinessId(businessId);
        reimbursement.setInstanceId(snapshot.getInstanceId());
        reimbursement.setBusinessStatus(resolveStatus(snapshot));
        reimbursementMapper.updateById(reimbursement);
        return toDetailVo(reimbursement);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void syncFromEvent(WorkflowChangedEvent event) {
        if (event == null || !ReimbursementConstants.FLOW_CODE.equals(event.flowCode())
                || !StringUtils.hasText(event.businessId())
                || event.operation() == WorkflowOperation.START
                || event.operation() == WorkflowOperation.ACTIVATE
                || event.operation() == WorkflowOperation.SUSPEND) {
            return;
        }
        WorkflowReimbursement reimbursement = reimbursementMapper.selectOne(
                WorkflowReimbursement::getBusinessId, event.businessId());
        if (reimbursement == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "工作流事件对应的报销单不存在");
        }
        reimbursement.setInstanceId(event.instanceId());
        reimbursement.setBusinessStatus(resolveStatus(event));
        reimbursementMapper.updateById(reimbursement);
    }

    private PageResponse<ReimbursementPageVo> pageInternal(ReimbursementSearchDto searchDto, Long applicantId) {
        ReimbursementSearchDto query = searchDto == null ? new ReimbursementSearchDto() : searchDto;
        LambdaQueryWrapperX<WorkflowReimbursement> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(WorkflowReimbursement::getApplicantId, applicantId)
                .eqIfPresent(WorkflowReimbursement::getBusinessStatus, query.getBusinessStatus());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(nested -> nested.like(WorkflowReimbursement::getBusinessId, query.getKeyword())
                    .or()
                    .like(WorkflowReimbursement::getTitle, query.getKeyword()));
        }
        wrapper.orderByDesc(WorkflowReimbursement::getUpdateTime)
                .orderByDesc(WorkflowReimbursement::getId);
        PageResponse<WorkflowReimbursement> data = reimbursementMapper.selectPage(query, wrapper);
        return new PageResponse<>(BeanUtil.copyToList(data.getList(), ReimbursementPageVo.class), data.getTotal());
    }

    private WorkflowReimbursement requireOwned(Long id) {
        WorkflowReimbursement reimbursement = requireEntity(id);
        if (!LoginUserInfoModelContext.getRequiredLoginUserId().equals(reimbursement.getApplicantId())) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "只能操作本人报销单");
        }
        return reimbursement;
    }

    private WorkflowReimbursement requireEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "报销单ID不能为空");
        }
        WorkflowReimbursement reimbursement = reimbursementMapper.selectById(id);
        if (reimbursement == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "报销单不存在");
        }
        return reimbursement;
    }

    private WorkflowReimbursement requireByBusinessId(String businessId) {
        WorkflowReimbursement reimbursement = reimbursementMapper.selectOne(
                WorkflowReimbursement::getBusinessId, businessId);
        if (reimbursement == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "报销单不存在");
        }
        return reimbursement;
    }

    private void requireEditable(WorkflowReimbursement reimbursement) {
        if (reimbursement.getBusinessStatus() == null || !reimbursement.getBusinessStatus().isEditable()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "当前报销单状态不允许修改或提交");
        }
    }

    private ReimbursementStatus resolveStatus(WorkflowChangedEvent event) {
        if (event.operation() == WorkflowOperation.CANCEL) {
            return ReimbursementStatus.CANCELLED;
        }
        if (event.operation() == WorkflowOperation.TERMINATE) {
            return ReimbursementStatus.TERMINATED;
        }
        if (event.operation() == WorkflowOperation.INVALIDATE) {
            return ReimbursementStatus.INVALIDATED;
        }
        // 只有退回申请人节点才恢复业务可编辑状态，中间节点退回仍处于审批中。
        if (event.operation() == WorkflowOperation.REJECT_LAST
                || event.operation() == WorkflowOperation.REJECT_TO) {
            return ReimbursementConstants.APPLICANT_NODE_CODE.equals(event.targetNodeCode())
                    ? ReimbursementStatus.RETURNED : ReimbursementStatus.APPROVING;
        }
        if (isFinished(event.flowStatus())
                || ReimbursementConstants.END_NODE_CODE.equals(event.targetNodeCode())) {
            return ReimbursementStatus.APPROVED;
        }
        return ReimbursementStatus.APPROVING;
    }

    private ReimbursementStatus resolveStatus(WorkflowInstanceSnapshot snapshot) {
        String flowStatus = snapshot.getFlowStatus();
        if (FLOW_STATUS_TERMINATE.equals(flowStatus)) {
            return ReimbursementStatus.TERMINATED;
        }
        if (FLOW_STATUS_NULLIFY.equals(flowStatus) || FLOW_STATUS_INVALID.equals(flowStatus)) {
            return ReimbursementStatus.INVALIDATED;
        }
        if (FLOW_STATUS_CANCEL.equals(flowStatus)) {
            return ReimbursementStatus.CANCELLED;
        }
        if (isFinished(flowStatus) || ReimbursementConstants.END_NODE_CODE.equals(snapshot.getNodeCode())) {
            return ReimbursementStatus.APPROVED;
        }
        if (ReimbursementConstants.APPLICANT_NODE_CODE.equals(snapshot.getNodeCode())
                && !FLOW_STATUS_TOBE_SUBMIT.equals(flowStatus)) {
            return ReimbursementStatus.RETURNED;
        }
        return FLOW_STATUS_TOBE_SUBMIT.equals(flowStatus)
                ? ReimbursementStatus.DRAFT : ReimbursementStatus.APPROVING;
    }

    private boolean isFinished(String flowStatus) {
        return FLOW_STATUS_PASS.equals(flowStatus) || FLOW_STATUS_AUTO_PASS.equals(flowStatus)
                || FLOW_STATUS_FINISHED.equals(flowStatus);
    }

    private ReimbursementDetailVo toDetailVo(WorkflowReimbursement reimbursement) {
        return BeanUtil.copyProperties(reimbursement, ReimbursementDetailVo.class);
    }
}
