package com.vita.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.support.WorkflowAdminAccessChecker;
import com.vita.workflow.flow.category.dto.WorkflowCategoryCreateDto;
import com.vita.workflow.flow.category.dto.WorkflowCategoryDeletedDto;
import com.vita.workflow.flow.category.dto.WorkflowCategoryUpdateDto;
import com.vita.workflow.flow.category.service.WorkflowCategoryManagementService;
import com.vita.workflow.flow.category.vo.WorkflowCategoryVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流分类管理接口
 * @Version: 1.0
 */
@Validated
@RestController
@ConditionalOnWorkflowEnabled
public class WorkflowCategoryController {

    private final WorkflowCategoryManagementService categoryService;

    public WorkflowCategoryController(
            WorkflowCategoryManagementService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 查询工作流分类树。
     *
     * @return 分类树
     */
    @GetMapping("/system/workflow/categories/tree")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_CATEGORY_VIEW_PERMISSION)
    public CommonResult<List<WorkflowCategoryVo>> tree() {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(categoryService.tree());
    }

    /**
     * 查询工作流分类详情。
     *
     * @param categoryId 分类 ID
     * @return 分类详情
     */
    @GetMapping("/system/workflow/categories/{categoryId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_CATEGORY_VIEW_PERMISSION)
    public CommonResult<WorkflowCategoryVo> detail(
            @PathVariable("categoryId") Long categoryId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(categoryService.detail(categoryId));
    }

    /**
     * 创建工作流分类。
     *
     * @param createDto 创建请求
     * @return 分类 ID
     */
    @RepeatSubmit
    @Log(module = "工作流分类", businessType = "CREATE")
    @PostMapping("/system/workflow/categories")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_CATEGORY_MANAGE_PERMISSION)
    public CommonResult<Long> create(
            @Valid @RequestBody WorkflowCategoryCreateDto createDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(categoryService.create(createDto));
    }

    /**
     * 更新工作流分类。
     *
     * @param categoryId 分类 ID
     * @param updateDto 更新请求
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流分类", businessType = "UPDATE")
    @PostMapping("/system/workflow/categories/{categoryId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_CATEGORY_MANAGE_PERMISSION)
    public CommonResult<Boolean> update(
            @PathVariable("categoryId") Long categoryId,
            @Valid @RequestBody WorkflowCategoryUpdateDto updateDto) {
        WorkflowAdminAccessChecker.check();
        categoryService.update(categoryId, updateDto);
        return CommonResult.success(true);
    }

    /**
     * 安全删除工作流分类。
     *
     * @param deletedDto 删除请求对象
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流分类", businessType = "DELETE")
    @PostMapping("/system/workflow/categories/delete")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_CATEGORY_MANAGE_PERMISSION)
    public CommonResult<Boolean> delete(
            @Valid @RequestBody WorkflowCategoryDeletedDto deletedDto) {
        WorkflowAdminAccessChecker.check();
        categoryService.delete(deletedDto);
        return CommonResult.success(true);
    }
}
