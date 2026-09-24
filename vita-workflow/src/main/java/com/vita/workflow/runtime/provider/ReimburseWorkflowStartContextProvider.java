package com.vita.workflow.runtime.provider;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.system.sysDept.entity.SysDept;
import com.vita.system.sysDept.service.ISysDeptService;
import com.vita.workflow.api.WorkflowStartContextProvider;
import com.vita.workflow.api.model.WorkflowStartContext;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.reimbursement.constant.ReimbursementConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.provider
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 报销审批流程启动上下文提供器
 * @Version: 1.0
 */
@Component
@ConditionalOnWorkflowEnabled
public class ReimburseWorkflowStartContextProvider implements WorkflowStartContextProvider {

    private final ISysDeptService sysDeptService;

    public ReimburseWorkflowStartContextProvider(ISysDeptService sysDeptService) {
        this.sysDeptService = sysDeptService;
    }

    @Override
    public boolean supports(String flowCode) {
        return ReimbursementConstants.FLOW_CODE.equals(flowCode);
    }

    @Override
    public Map<String, Object> provide(WorkflowStartContext context) {
        if (context.deptId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "发起人未归属部门，无法发起报销流程");
        }
        SysDept dept = sysDeptService.getById(context.deptId());
        if (dept == null || dept.getLeaderUserId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "发起人部门未配置负责人，无法发起报销流程");
        }
        return Map.of(
                WorkflowConstants.APPLICANT_PERMISSION_VARIABLE,
                WorkflowConstants.buildUserPermission(context.operatorId()),
                WorkflowConstants.DEPT_LEADER_PERMISSION_VARIABLE,
                WorkflowConstants.buildUserPermission(dept.getLeaderUserId())
        );
    }
}
