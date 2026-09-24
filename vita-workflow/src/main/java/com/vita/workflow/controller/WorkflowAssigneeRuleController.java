package com.vita.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.support.WorkflowAdminAccessChecker;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleCreateDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleDeletedDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleUpdateDto;
import com.vita.workflow.flow.rule.service.WorkflowAssigneeRuleService;
import com.vita.workflow.flow.rule.vo.WorkflowAssigneeRuleVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则管理接口
 * @Version: 1.0
 */
@Validated
@RestController
@ConditionalOnWorkflowEnabled
public class WorkflowAssigneeRuleController {

    private final WorkflowAssigneeRuleService ruleService;

    public WorkflowAssigneeRuleController(
            WorkflowAssigneeRuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * 查询受控办理人规则。
     *
     * @return 规则列表
     */
    @GetMapping("/system/workflow/assignee-rules")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_ASSIGNEE_RULE_VIEW_PERMISSION)
    public CommonResult<List<WorkflowAssigneeRuleVo>> list() {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(ruleService.list(false));
    }

    /**
     * 查询受控办理人规则详情。
     *
     * @param ruleId 规则 ID
     * @return 规则详情
     */
    @GetMapping("/system/workflow/assignee-rules/{ruleId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_ASSIGNEE_RULE_VIEW_PERMISSION)
    public CommonResult<WorkflowAssigneeRuleVo> detail(
            @PathVariable("ruleId") Long ruleId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(ruleService.detail(ruleId));
    }

    /**
     * 创建受控办理人规则。
     *
     * @param createDto 创建请求
     * @return 规则 ID
     */
    @RepeatSubmit
    @Log(module = "工作流办理人规则", businessType = "CREATE")
    @PostMapping("/system/workflow/assignee-rules")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_ASSIGNEE_RULE_MANAGE_PERMISSION)
    public CommonResult<Long> create(
            @Valid @RequestBody WorkflowAssigneeRuleCreateDto createDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(ruleService.create(createDto));
    }

    /**
     * 更新受控办理人规则。
     *
     * @param ruleId 规则 ID
     * @param updateDto 更新请求
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流办理人规则", businessType = "UPDATE")
    @PostMapping("/system/workflow/assignee-rules/{ruleId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_ASSIGNEE_RULE_MANAGE_PERMISSION)
    public CommonResult<Boolean> update(
            @PathVariable("ruleId") Long ruleId,
            @Valid @RequestBody WorkflowAssigneeRuleUpdateDto updateDto) {
        WorkflowAdminAccessChecker.check();
        ruleService.update(ruleId, updateDto);
        return CommonResult.success(true);
    }

    /**
     * 删除受控办理人规则。
     *
     * @param deletedDto 删除请求对象
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流办理人规则", businessType = "DELETE")
    @PostMapping("/system/workflow/assignee-rules/delete")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_ASSIGNEE_RULE_MANAGE_PERMISSION)
    public CommonResult<Boolean> delete(
            @Valid @RequestBody WorkflowAssigneeRuleDeletedDto deletedDto) {
        WorkflowAdminAccessChecker.check();
        ruleService.delete(deletedDto);
        return CommonResult.success(true);
    }
}
