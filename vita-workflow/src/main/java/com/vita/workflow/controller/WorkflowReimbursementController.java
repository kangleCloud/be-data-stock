package com.vita.workflow.controller;

import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.reimbursement.dto.*;
import com.vita.workflow.reimbursement.service.WorkflowReimbursementService;
import com.vita.workflow.reimbursement.vo.ReimbursementDetailVo;
import com.vita.workflow.reimbursement.vo.ReimbursementPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 普通用户报销工作流接口
 * @Version: 1.0
 */
@Validated
@RestController
@ConditionalOnWorkflowEnabled
@RequestMapping("/workflow/reimbursements")
public class WorkflowReimbursementController {

    private final WorkflowReimbursementService reimbursementService;

    public WorkflowReimbursementController(WorkflowReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    /**
     * 创建报销草稿。
     *
     * @param createDto 创建参数
     * @return 报销单 ID
     */
    @RepeatSubmit
    @Log(module = "工作流报销", businessType = "CREATE")
    @PostMapping
    public CommonResult<Long> create(@Valid @RequestBody ReimbursementCreateDto createDto) {
        return CommonResult.success(reimbursementService.create(createDto));
    }

    /**
     * 修改本人草稿或退回单。
     *
     * @param id 报销单 ID
     * @param updateDto 修改参数
     * @return 修改结果
     */
    @RepeatSubmit
    @Log(module = "工作流报销", businessType = "UPDATE")
    @PostMapping("/{id}")
    public CommonResult<Boolean> update(@PathVariable Long id,
                                        @Valid @RequestBody ReimbursementUpdateDto updateDto) {
        reimbursementService.update(id, updateDto);
        return CommonResult.success(true);
    }

    /**
     * 分页查询我的报销单。
     *
     * @param searchDto 查询参数
     * @return 分页结果
     */
    @GetMapping("/mine")
    public CommonResult<PageResponse<ReimbursementPageVo>> mine(@Valid ReimbursementSearchDto searchDto) {
        return CommonResult.success(reimbursementService.mine(searchDto));
    }

    /**
     * 查询有权访问的报销单详情。
     *
     * @param id 报销单 ID
     * @return 报销单详情
     */
    @GetMapping("/{id}")
    public CommonResult<ReimbursementDetailVo> detail(@PathVariable Long id) {
        return CommonResult.success(reimbursementService.detail(id, false));
    }

    /**
     * 首次启动并提交，或再次提交退回任务。
     *
     * @param id 报销单 ID
     * @param submitDto 提交参数
     * @return 报销单详情
     */
    @RepeatSubmit
    @Log(module = "工作流报销", businessType = "SUBMIT")
    @PostMapping("/{id}/submit")
    public CommonResult<ReimbursementDetailVo> submit(@PathVariable Long id,
                                                       @Valid @RequestBody(required = false)
                                                       ReimbursementSubmitDto submitDto) {
        return CommonResult.success(reimbursementService.submit(id, submitDto));
    }

    /**
     * 在审批人尚未办理时撤回报销单。
     *
     * @param id 报销单 ID
     * @param withdrawDto 撤回参数
     * @return 报销单详情
     */
    @RepeatSubmit
    @Log(module = "工作流报销", businessType = "WITHDRAW")
    @PostMapping("/{id}/withdraw")
    public CommonResult<ReimbursementDetailVo> withdraw(@PathVariable Long id,
                                                         @Valid @RequestBody(required = false)
                                                         ReimbursementWithdrawDto withdrawDto) {
        return CommonResult.success(reimbursementService.withdraw(id, withdrawDto));
    }
}
