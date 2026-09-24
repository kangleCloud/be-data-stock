package com.vita.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.api.command.WorkflowNextNodeCommand;
import com.vita.workflow.api.command.WorkflowRejectCommand;
import com.vita.workflow.api.command.WorkflowTaskCommand;
import com.vita.workflow.api.command.WorkflowTaskOperationCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowNodeSnapshot;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.dto.*;
import com.vita.workflow.controller.support.WorkflowAdminAccessChecker;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowCopyVo;
import com.vita.workflow.runtime.vo.WorkflowTaskHandlerVo;
import com.vita.workflow.runtime.vo.WorkflowTaskVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流任务运行与管理接口
 * @Version: 1.0
 */
@Validated
@RestController
@ConditionalOnWorkflowEnabled
public class WorkflowTaskController {

    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowTaskController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    /**
     * 分页查询当前用户待办任务。
     *
     * @param searchDto 查询条件
     * @return 待办任务分页结果
     */
    @GetMapping("/workflow/tasks/pending")
    public CommonResult<PageResponse<WorkflowTaskVo>> pending(
            @Valid WorkflowTaskSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.pendingTasks(searchDto));
    }

    /**
     * 分页查询当前用户已办任务。
     *
     * @param searchDto 查询条件
     * @return 已办任务分页结果
     */
    @GetMapping("/workflow/tasks/completed")
    public CommonResult<PageResponse<WorkflowTaskVo>> completed(
            @Valid WorkflowTaskSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.completedTasks(searchDto));
    }

    /**
     * 查询当前用户可办理的任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    @GetMapping("/workflow/tasks/{taskId}")
    public CommonResult<WorkflowTaskVo> detail(@PathVariable("taskId") Long taskId) {
        return CommonResult.success(workflowRuntimeService.taskDetail(taskId));
    }

    /**
     * 查询当前任务候选办理人。
     *
     * @param taskId 任务 ID
     * @return 办理人列表
     */
    @GetMapping("/workflow/tasks/{taskId}/handlers")
    public CommonResult<List<WorkflowTaskHandlerVo>> handlers(
            @PathVariable("taskId") Long taskId) {
        return CommonResult.success(workflowRuntimeService.taskHandlers(taskId));
    }

    /**
     * 通过当前任务。
     *
     * @param taskId 任务 ID
     * @param request 办理请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "PASS")
    @PostMapping("/workflow/tasks/{taskId}/pass")
    public CommonResult<WorkflowInstanceSnapshot> pass(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowActionRequest request) {
        return CommonResult.success(workflowRuntimeService.pass(toTaskCommand(taskId, request)));
    }

    /**
     * 退回上一人工节点。
     *
     * @param taskId 任务 ID
     * @param request 办理请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "REJECT_LAST")
    @PostMapping("/workflow/tasks/{taskId}/reject-last")
    public CommonResult<WorkflowInstanceSnapshot> rejectLast(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowActionRequest request) {
        return CommonResult.success(workflowRuntimeService.rejectLast(toTaskCommand(taskId, request)));
    }

    /**
     * 退回定义允许的指定节点。
     *
     * @param taskId 任务 ID
     * @param request 指定节点退回请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "REJECT_TO")
    @PostMapping("/workflow/tasks/{taskId}/reject-to")
    public CommonResult<WorkflowInstanceSnapshot> rejectTo(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowRejectToRequest request) {
        WorkflowRejectCommand command = new WorkflowRejectCommand();
        command.setTaskId(taskId);
        command.setTargetNodeCode(request.getTargetNodeCode());
        command.setMessage(request.getMessage());
        command.setVariables(request.getVariables());
        command.setAttachmentRef(request.getAttachmentRef());
        command.setCopyUserIds(request.getCopyUserIds());
        return CommonResult.success(workflowRuntimeService.rejectTo(command));
    }

    /**
     * 预览任务办理后的下一节点和动态选人要求。
     *
     * @param taskId 任务 ID
     * @param request 下一节点预览请求
     * @return 下一节点列表
     */
    @PostMapping("/workflow/tasks/{taskId}/next-nodes")
    public CommonResult<List<WorkflowNodeSnapshot>> nextNodes(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowNextNodeRequest request) {
        WorkflowNextNodeCommand command = new WorkflowNextNodeCommand();
        command.setTaskId(taskId);
        command.setVariables(request.getVariables());
        return CommonResult.success(workflowRuntimeService.previewNextNodes(command));
    }

    /**
     * 查询当前任务允许退回的节点。
     *
     * @param taskId 任务 ID
     * @return 可退回节点列表
     */
    @GetMapping("/workflow/tasks/{taskId}/rejectable-nodes")
    public CommonResult<List<WorkflowNodeSnapshot>> rejectableNodes(
            @PathVariable("taskId") Long taskId) {
        return CommonResult.success(workflowRuntimeService.rejectableNodes(taskId));
    }

    /**
     * 转办当前任务。
     *
     * @param taskId 任务 ID
     * @param request 高级任务操作请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "TRANSFER")
    @PostMapping("/workflow/tasks/{taskId}/transfer")
    public CommonResult<WorkflowInstanceSnapshot> transfer(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowTaskOperationRequest request) {
        return CommonResult.success(workflowRuntimeService.transfer(toOperationCommand(taskId, request)));
    }

    /**
     * 委派当前任务。
     *
     * @param taskId 任务 ID
     * @param request 高级任务操作请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "DELEGATE")
    @PostMapping("/workflow/tasks/{taskId}/delegate")
    public CommonResult<WorkflowInstanceSnapshot> delegate(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowTaskOperationRequest request) {
        return CommonResult.success(workflowRuntimeService.delegate(toOperationCommand(taskId, request)));
    }

    /**
     * 为当前任务增加办理人。
     *
     * @param taskId 任务 ID
     * @param request 高级任务操作请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "ADD_SIGN")
    @PostMapping("/workflow/tasks/{taskId}/add-sign")
    public CommonResult<WorkflowInstanceSnapshot> addSign(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowTaskOperationRequest request) {
        return CommonResult.success(workflowRuntimeService.addSign(toOperationCommand(taskId, request)));
    }

    /**
     * 移除当前任务运行期增加的办理人。
     *
     * @param taskId 任务 ID
     * @param request 高级任务操作请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "REDUCE_SIGN")
    @PostMapping("/workflow/tasks/{taskId}/reduce-sign")
    public CommonResult<WorkflowInstanceSnapshot> reduceSign(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowTaskOperationRequest request) {
        return CommonResult.success(workflowRuntimeService.reduceSign(toOperationCommand(taskId, request)));
    }

    /**
     * 终止当前任务所属流程。
     *
     * @param taskId 任务 ID
     * @param request 高级任务操作请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "TERMINATE")
    @PostMapping("/workflow/tasks/{taskId}/terminate")
    public CommonResult<WorkflowInstanceSnapshot> terminate(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowTaskOperationRequest request) {
        return CommonResult.success(workflowRuntimeService.terminate(toOperationCommand(taskId, request)));
    }

    /**
     * 分页查询当前用户收到的抄送记录。
     *
     * @param searchDto 查询条件
     * @return 抄送记录分页结果
     */
    @GetMapping("/workflow/tasks/copied")
    public CommonResult<PageResponse<WorkflowCopyVo>> copied(
            @Valid WorkflowTaskSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.copiedTasks(searchDto));
    }

    /**
     * 管理端分页查询全部待办任务。
     *
     * @param searchDto 查询条件
     * @return 待办任务分页结果
     */
    @GetMapping("/system/workflow/tasks/pending")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_TASK_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowTaskVo>> allPending(
            @Valid WorkflowTaskSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.allPendingTasks(searchDto));
    }

    /**
     * 管理端分页查询全部已办任务。
     *
     * @param searchDto 查询条件
     * @return 已办任务分页结果
     */
    @GetMapping("/system/workflow/tasks/completed")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_TASK_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowTaskVo>> allCompleted(
            @Valid WorkflowTaskSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.allCompletedTasks(searchDto));
    }

    /**
     * 管理端将当前任务办理人替换为启用用户。
     *
     * @param taskId 任务 ID
     * @param request 办理人修改请求
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流任务", businessType = "UPDATE_HANDLER")
    @PostMapping("/system/workflow/tasks/{taskId}/handlers")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_TASK_MANAGE_PERMISSION)
    public CommonResult<Boolean> updateHandlers(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody WorkflowTaskHandlersUpdateRequest request) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.updateTaskHandlers(
                taskId, request.getUserIds(), request.getReason()));
    }

    /**
     * 将 HTTP 办理请求转换为引擎中立任务命令。
     *
     * @param taskId 任务 ID
     * @param request 办理请求
     * @return 任务命令
     */
    private WorkflowTaskCommand toTaskCommand(Long taskId, WorkflowActionRequest request) {
        WorkflowTaskCommand command = new WorkflowTaskCommand();
        command.setTaskId(taskId);
        command.setMessage(request.getMessage());
        command.setVariables(request.getVariables());
        command.setAttachmentRef(request.getAttachmentRef());
        command.setNextAssignees(request.getNextAssignees());
        command.setCopyUserIds(request.getCopyUserIds());
        return command;
    }

    /**
     * 将 HTTP 高级任务请求转换为引擎中立操作命令。
     *
     * @param taskId 任务 ID
     * @param request 高级任务操作请求
     * @return 高级任务操作命令
     */
    private WorkflowTaskOperationCommand toOperationCommand(
            Long taskId, WorkflowTaskOperationRequest request) {
        WorkflowTaskOperationCommand command = new WorkflowTaskOperationCommand();
        command.setTaskId(taskId);
        command.setUserIds(request.getUserIds());
        command.setMessage(request.getMessage());
        command.setAttachmentRef(request.getAttachmentRef());
        return command;
    }
}
