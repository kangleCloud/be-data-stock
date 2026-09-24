package com.vita.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.workflow.api.command.WorkflowCancelCommand;
import com.vita.workflow.api.command.WorkflowStartCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowStartResult;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.dto.WorkflowActivityRequest;
import com.vita.workflow.controller.dto.WorkflowCancelRequest;
import com.vita.workflow.controller.dto.WorkflowInvalidateRequest;
import com.vita.workflow.controller.dto.WorkflowVariableUpdateRequest;
import com.vita.workflow.controller.support.WorkflowAdminAccessChecker;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.vo.WorkflowHistoryVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceGraphVo;
import com.vita.workflow.runtime.vo.WorkflowInstanceVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流实例运行与管理接口
 * @Version: 1.0
 */
@Validated
@RestController
@ConditionalOnWorkflowEnabled
public class WorkflowInstanceController {

    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowInstanceController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    /**
     * 创建待提交流程实例。
     *
     * @param command 流程启动命令
     * @return 流程启动结果
     */
    @RepeatSubmit
    @Log(module = "工作流实例", businessType = "START")
    @PostMapping("/workflow/instances/start")
    public CommonResult<WorkflowStartResult> start(
            @Valid @RequestBody WorkflowStartCommand command) {
        return CommonResult.success(workflowRuntimeService.start(command));
    }

    /**
     * 分页查询当前用户发起的流程。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @GetMapping("/workflow/instances/mine")
    public CommonResult<PageResponse<WorkflowInstanceVo>> mine(
            @Valid WorkflowInstanceSearchDto searchDto) {
        return CommonResult.success(workflowRuntimeService.myInstances(searchDto));
    }

    /**
     * 查询当前用户有权访问的流程实例。
     *
     * @param instanceId 流程实例 ID
     * @return 流程实例详情
     */
    @GetMapping("/workflow/instances/{instanceId}")
    public CommonResult<WorkflowInstanceVo> instanceDetail(
            @PathVariable("instanceId") Long instanceId) {
        return CommonResult.success(workflowRuntimeService.instanceDetail(instanceId, false));
    }

    /**
     * 查询当前用户有权访问的流程轨迹。
     *
     * @param instanceId 流程实例 ID
     * @return 流程轨迹
     */
    @GetMapping("/workflow/instances/{instanceId}/history")
    public CommonResult<List<WorkflowHistoryVo>> instanceHistory(
            @PathVariable("instanceId") Long instanceId) {
        return CommonResult.success(workflowRuntimeService.instanceHistory(instanceId, false));
    }

    /**
     * 根据业务 ID 查询当前用户有权访问的流程实例。
     *
     * @param businessId 业务 ID
     * @return 流程实例详情
     */
    @GetMapping("/workflow/instances/by-business/{businessId}")
    public CommonResult<WorkflowInstanceVo> instanceByBusinessId(
            @PathVariable("businessId") String businessId) {
        return CommonResult.success(
                workflowRuntimeService.instanceByBusinessId(businessId, false));
    }

    /**
     * 根据业务 ID 查询当前用户有权访问的流程轨迹。
     *
     * @param businessId 业务 ID
     * @return 流程轨迹
     */
    @GetMapping("/workflow/instances/by-business/{businessId}/history")
    public CommonResult<List<WorkflowHistoryVo>> instanceHistoryByBusinessId(
            @PathVariable("businessId") String businessId) {
        return CommonResult.success(
                workflowRuntimeService.instanceHistoryByBusinessId(businessId, false));
    }

    /**
     * 根据业务 ID 查询当前用户有权访问的脱敏流程图。
     *
     * @param businessId 业务 ID
     * @return 流程图
     */
    @GetMapping("/workflow/instances/by-business/{businessId}/graph")
    public CommonResult<WorkflowInstanceGraphVo> instanceGraphByBusinessId(
            @PathVariable("businessId") String businessId) {
        return CommonResult.success(
                workflowRuntimeService.instanceGraphByBusinessId(businessId, false));
    }

    /**
     * 在审批人尚未办理时撤回流程。
     *
     * @param instanceId 流程实例 ID
     * @param request 撤回请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流实例", businessType = "CANCEL")
    @PostMapping("/workflow/instances/{instanceId}/cancel")
    public CommonResult<WorkflowInstanceSnapshot> cancel(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody WorkflowCancelRequest request) {
        WorkflowCancelCommand command = new WorkflowCancelCommand();
        command.setInstanceId(instanceId);
        command.setMessage(request.getMessage());
        return CommonResult.success(workflowRuntimeService.cancel(command));
    }

    /**
     * 管理端分页查询运行中的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @GetMapping("/system/workflow/instances/running")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowInstanceVo>> runningInstances(
            @Valid WorkflowInstanceSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.runningInstances(searchDto));
    }

    /**
     * 管理端分页查询已完成的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    @GetMapping("/system/workflow/instances/finished")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<PageResponse<WorkflowInstanceVo>> finishedInstances(
            @Valid WorkflowInstanceSearchDto searchDto) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.finishedInstances(searchDto));
    }

    /**
     * 管理端查询流程实例详情。
     *
     * @param instanceId 流程实例 ID
     * @return 流程实例详情
     */
    @GetMapping("/system/workflow/instances/{instanceId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<WorkflowInstanceVo> managedInstanceDetail(
            @PathVariable("instanceId") Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.instanceDetail(instanceId, true));
    }

    /**
     * 管理端查询流程轨迹。
     *
     * @param instanceId 流程实例 ID
     * @return 流程轨迹
     */
    @GetMapping("/system/workflow/instances/{instanceId}/history")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<List<WorkflowHistoryVo>> managedInstanceHistory(
            @PathVariable("instanceId") Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.instanceHistory(instanceId, true));
    }

    /**
     * 管理端根据业务 ID 查询流程实例。
     *
     * @param businessId 业务 ID
     * @return 流程实例详情
     */
    @GetMapping("/system/workflow/instances/by-business/{businessId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<WorkflowInstanceVo> managedInstanceByBusinessId(
            @PathVariable("businessId") String businessId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.instanceByBusinessId(businessId, true));
    }

    /**
     * 管理端根据业务 ID 查询流程轨迹。
     *
     * @param businessId 业务 ID
     * @return 流程轨迹
     */
    @GetMapping("/system/workflow/instances/by-business/{businessId}/history")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<List<WorkflowHistoryVo>> managedInstanceHistoryByBusinessId(
            @PathVariable("businessId") String businessId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.instanceHistoryByBusinessId(businessId, true));
    }

    /**
     * 管理端根据业务 ID 查询脱敏流程图。
     *
     * @param businessId 业务 ID
     * @return 流程图
     */
    @GetMapping("/system/workflow/instances/by-business/{businessId}/graph")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<WorkflowInstanceGraphVo> managedInstanceGraphByBusinessId(
            @PathVariable("businessId") String businessId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.instanceGraphByBusinessId(businessId, true));
    }

    /**
     * 管理端查询过滤后的流程变量。
     *
     * @param instanceId 流程实例 ID
     * @return 流程变量
     */
    @GetMapping("/system/workflow/instances/{instanceId}/variables")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_VIEW_PERMISSION)
    public CommonResult<Map<String, Object>> instanceVariables(
            @PathVariable("instanceId") Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.instanceVariables(instanceId));
    }

    /**
     * 管理端按流程白名单修改实例变量。
     *
     * @param instanceId 流程实例 ID
     * @param request 变量修改请求
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流实例", businessType = "UPDATE_VARIABLE")
    @PostMapping("/system/workflow/instances/{instanceId}/variables")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION)
    public CommonResult<Boolean> updateInstanceVariable(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody WorkflowVariableUpdateRequest request) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(workflowRuntimeService.updateInstanceVariable(
                instanceId, request.getKey(), request.getValue(), request.getReason()));
    }

    /**
     * 管理端激活或挂起流程实例。
     *
     * @param instanceId 流程实例 ID
     * @param request 激活状态请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流实例", businessType = "UPDATE_ACTIVITY")
    @PostMapping("/system/workflow/instances/{instanceId}/activity")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION)
    public CommonResult<WorkflowInstanceSnapshot> setInstanceActivity(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody WorkflowActivityRequest request) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.setInstanceActive(instanceId, request.getActive()));
    }

    /**
     * 管理端安全作废流程实例。
     *
     * @param instanceId 流程实例 ID
     * @param request 作废请求
     * @return 流程实例快照
     */
    @RepeatSubmit
    @Log(module = "工作流实例", businessType = "INVALIDATE")
    @PostMapping("/system/workflow/instances/{instanceId}/invalidate")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION)
    public CommonResult<WorkflowInstanceSnapshot> invalidateInstance(
            @PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody WorkflowInvalidateRequest request) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.invalidateInstance(instanceId, request.getMessage()));
    }

    /**
     * 管理端物理删除未结束且无业务绑定的实例。
     *
     * @param instanceId 流程实例 ID
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流实例", businessType = "DELETE")
    @PostMapping("/system/workflow/instances/{instanceId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION)
    public CommonResult<Boolean> deleteRunningInstance(
            @PathVariable("instanceId") Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.deleteRunningInstance(instanceId));
    }

    /**
     * 管理端根据业务 ID 物理删除未结束且无业务绑定的实例。
     *
     * @param businessId 业务 ID
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流实例", businessType = "DELETE")
    @PostMapping("/system/workflow/instances/by-business/{businessId}")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION)
    public CommonResult<Boolean> deleteRunningInstanceByBusinessId(
            @PathVariable("businessId") String businessId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.deleteRunningInstanceByBusinessId(businessId));
    }

    /**
     * 管理端物理删除已结束且无业务绑定的实例历史。
     *
     * @param instanceId 流程实例 ID
     * @return 操作结果
     */
    @RepeatSubmit
    @Log(module = "工作流实例", businessType = "DELETE_HISTORY")
    @PostMapping("/system/workflow/instances/{instanceId}/history")
    @SaCheckPermission(WorkflowConstants.WORKFLOW_INSTANCE_MANAGE_PERMISSION)
    public CommonResult<Boolean> deleteFinishedInstanceHistory(
            @PathVariable("instanceId") Long instanceId) {
        WorkflowAdminAccessChecker.check();
        return CommonResult.success(
                workflowRuntimeService.deleteFinishedInstanceHistory(instanceId));
    }
}
