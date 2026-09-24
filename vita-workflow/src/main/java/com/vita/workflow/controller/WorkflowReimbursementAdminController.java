package com.vita.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.support.WorkflowAdminAccessChecker;
import com.vita.workflow.reimbursement.dto.ReimbursementSearchDto;
import com.vita.workflow.reimbursement.service.WorkflowReimbursementService;
import com.vita.workflow.reimbursement.vo.ReimbursementDetailVo;
import com.vita.workflow.reimbursement.vo.ReimbursementPageVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 管理端报销工作流查询和状态补偿接口
 * @Version: 1.0
 */
@Validated
@RestController
@ConditionalOnWorkflowEnabled
@RequestMapping("/system/workflow/reimbursements")
public class WorkflowReimbursementAdminController {

    private final WorkflowReimbursementService reimbursementService;

    public WorkflowReimbursementAdminController(WorkflowReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    /**
     * 分页查询全部报销单。
     *
     * @param searchDto 查询参数
     * @return 分页结果
     */
    @GetMapping("/page")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_REIMBURSEMENT_VIEW_PERMISSION)
    public CommonResult<PageResponse<ReimbursementPageVo>> page(@Valid ReimbursementSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(reimbursementService.page(searchDto));
    }

    /**
     * 查询报销单详情。
     *
     * @param id 报销单 ID
     * @return 报销单详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_REIMBURSEMENT_VIEW_PERMISSION)
    public CommonResult<ReimbursementDetailVo> detail(@PathVariable Long id) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(reimbursementService.detail(id, true));
    }

    /**
     * 根据业务 ID 从流程实例重同步报销状态。
     *
     * @param businessId 全局业务 ID
     * @return 重同步后的报销单详情
     */
    @RepeatSubmit
    @Log(module = "工作流报销", businessType = "RESYNC")
    @PostMapping("/resync")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_REIMBURSEMENT_RESYNC_PERMISSION)
    public CommonResult<ReimbursementDetailVo> resync(
            @RequestParam("businessId") @NotBlank(message = "业务ID不能为空") String businessId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(reimbursementService.resync(businessId));
    }
}
