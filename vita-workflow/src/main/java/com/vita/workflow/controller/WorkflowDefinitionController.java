package com.vita.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.dto.WorkflowActivityRequest;
import com.vita.workflow.controller.support.WorkflowAdminAccessChecker;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowDefinitionVo;
import com.vita.workflow.service.IWorkflowDefinitionService;
import jakarta.validation.Valid;
import org.dromara.warm.flow.core.dto.DefJson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流定义管理接口
 * @Version: 1.0
 */
@Validated
@RestController
@ConditionalOnWorkflowEnabled
public class WorkflowDefinitionController {

    private final WorkflowRuntimeService workflowRuntimeService;

    private final IWorkflowDefinitionService workflowDefinitionService;

    public WorkflowDefinitionController(WorkflowRuntimeService workflowRuntimeService,
                                        IWorkflowDefinitionService workflowDefinitionService) {
        this.workflowRuntimeService = workflowRuntimeService;
        this.workflowDefinitionService = workflowDefinitionService;
    }

    /**
     * 分页查询已发布流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    @GetMapping("/workflow/definitions/published")
    public CommonResult<PageResponse<WorkflowDefinitionVo>> published(
            @Valid WorkflowDefinitionSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.publishedDefinitions(searchDto));
    }

    /**
     * 管理端分页查询流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    @GetMapping("/system/workflow/definitions/page")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowDefinitionVo>> definitionPage(
            @Valid WorkflowDefinitionSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.definitionPage(searchDto));
    }

    /**
     * 管理端查询流程定义详情。
     *
     * @param definitionId 流程定义 ID
     * @return 流程定义详情
     */
    @GetMapping("/system/workflow/definitions/{definitionId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION)
    public CommonResult<WorkflowDefinitionVo> definitionDetail(
            @PathVariable("definitionId") Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.definitionDetail(definitionId));
    }

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 发布结果
     */
    @RepeatSubmit
    @Log(module = "工作流定义", businessType = "PUBLISH")
    @PostMapping("/system/workflow/definitions/{definitionId}/publish")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> publishDefinition(
            @PathVariable("definitionId") Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.publishDefinition(definitionId));
    }

    /**
     * 取消发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 取消发布结果
     */
    @RepeatSubmit
    @Log(module = "工作流定义", businessType = "UNPUBLISH")
    @PostMapping("/system/workflow/definitions/{definitionId}/unpublish")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> unpublishDefinition(
            @PathVariable("definitionId") Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.unpublishDefinition(definitionId));
    }

    /**
     * 复制流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 复制结果
     */
    @RepeatSubmit
    @Log(module = "工作流定义", businessType = "COPY")
    @PostMapping("/system/workflow/definitions/{definitionId}/copy")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> copyDefinition(
            @PathVariable("definitionId") Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.copyDefinition(definitionId));
    }

    /**
     * 安全删除流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 删除结果
     */
    @RepeatSubmit
    @Log(module = "工作流定义", businessType = "DELETE")
    @PostMapping("/system/workflow/definitions/{definitionId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> deleteDefinition(
            @PathVariable("definitionId") Long definitionId) {
        WorkflowAdminAccessChecker.check();
        workflowRuntimeService.deleteDefinition(definitionId);
        return CommonResult.success(true);
    }

    /**
     * 查看流程定义 JSON。
     *
     * @param definitionId 流程定义 ID
     * @return 流程定义 JSON
     */
    @GetMapping("/system/workflow/definitions/{definitionId}/json")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_VIEW_PERMISSION)
    public CommonResult<String> definitionJson(
            @PathVariable("definitionId") Long definitionId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.exportDefinitionJson(definitionId));
    }

    /**
     * 下载流程定义 JSON。
     *
     * @param definitionId 流程定义 ID
     * @return JSON 文件响应
     */
    @GetMapping("/system/workflow/definitions/{definitionId}/export")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public ResponseEntity<byte[]> exportDefinition(
            @PathVariable("definitionId") Long definitionId) {
        WorkflowAdminAccessChecker.check();
        byte[] content = workflowRuntimeService.exportDefinitionJson(definitionId)
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"workflow-" + definitionId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(content);
    }

    /**
     * 激活或挂起流程定义。
     *
     * @param definitionId 流程定义 ID
     * @param request 激活状态请求
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流定义", businessType = "UPDATE_ACTIVITY")
    @PostMapping("/system/workflow/definitions/{definitionId}/activity")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_DEFINITION_MANAGE_PERMISSION)
    public CommonResult<Boolean> setDefinitionActivity(
            @PathVariable("definitionId") Long definitionId,
            @Valid @RequestBody WorkflowActivityRequest request) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.setDefinitionActive(definitionId, request.getActive()));
    }

    /**
     * 导入 Warm-Flow 流程定义 JSON。
     *
     * @param defJson 流程定义对象
     * @return 导入结果
     */
    @RepeatSubmit
    @Log(module = "工作流定义", businessType = "IMPORT")
    @PostMapping(value = "/system/workflow/importJson", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SaCheckPermission(WorkflowConstants.WORKFLOW_IMPORT_PERMISSION)
    public CommonResult<Boolean> importJson(@RequestBody(required = false) DefJson defJson) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowDefinitionService.importJson(defJson));
    }
}
