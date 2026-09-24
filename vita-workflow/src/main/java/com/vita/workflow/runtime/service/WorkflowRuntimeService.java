package com.vita.workflow.runtime.service;

import com.vita.core.page.PageResponse;
import com.vita.workflow.api.WorkflowPort;
import com.vita.workflow.api.WorkflowTaskOperationPort;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.vo.*;

import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.service
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流运行时查询与管理服务
 * @Version: 1.0
 */
public interface WorkflowRuntimeService extends WorkflowPort, WorkflowTaskOperationPort {

    /**
     * 分页查询可发起的已发布流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    PageResponse<WorkflowDefinitionVo> publishedDefinitions(WorkflowDefinitionSearchDto searchDto);

    /**
     * 管理端分页查询全部流程定义。
     *
     * @param searchDto 查询条件
     * @return 流程定义分页结果
     */
    PageResponse<WorkflowDefinitionVo> definitionPage(WorkflowDefinitionSearchDto searchDto);

    /**
     * 查询流程定义详情。
     *
     * @param definitionId 流程定义 ID
     * @return 流程定义详情
     */
    WorkflowDefinitionVo definitionDetail(Long definitionId);

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 发布结果
     */
    boolean publishDefinition(Long definitionId);

    /**
     * 取消发布流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 取消发布结果
     */
    boolean unpublishDefinition(Long definitionId);

    /**
     * 复制流程定义。
     *
     * @param definitionId 流程定义 ID
     * @return 复制结果
     */
    boolean copyDefinition(Long definitionId);

    /**
     * 导出流程定义JSON。
     *
     * @param definitionId 流程定义ID
     * @return 流程定义JSON
     */
    String exportDefinitionJson(Long definitionId);

    /**
     * 激活或挂起流程定义。
     *
     * @param definitionId 流程定义ID
     * @param active true激活，false挂起
     * @return 操作结果
     */
    boolean setDefinitionActive(Long definitionId, boolean active);

    /**
     * 删除尚未产生实例的流程定义。
     *
     * @param definitionId 流程定义 ID
     */
    void deleteDefinition(Long definitionId);

    /**
     * 分页查询当前用户发起的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    PageResponse<WorkflowInstanceVo> myInstances(WorkflowInstanceSearchDto searchDto);

    /**
     * 管理端分页查询运行中的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    PageResponse<WorkflowInstanceVo> runningInstances(WorkflowInstanceSearchDto searchDto);

    /**
     * 管理端分页查询已完成的流程实例。
     *
     * @param searchDto 查询条件
     * @return 流程实例分页结果
     */
    PageResponse<WorkflowInstanceVo> finishedInstances(WorkflowInstanceSearchDto searchDto);

    /**
     * 查询流程实例详情。
     *
     * @param instanceId 流程实例 ID
     * @param administrator 是否按管理员身份跳过参与人校验
     * @return 流程实例详情
     */
    WorkflowInstanceVo instanceDetail(Long instanceId, boolean administrator);

    /**
     * 查询流程实例轨迹。
     *
     * @param instanceId 流程实例 ID
     * @param administrator 是否按管理员身份跳过参与人校验
     * @return 流程轨迹
     */
    List<WorkflowHistoryVo> instanceHistory(Long instanceId, boolean administrator);

    /**
     * 根据业务 ID 查询流程实例。
     *
     * @param businessId 业务 ID
     * @param administrator 是否按管理员身份跳过参与人校验
     * @return 流程实例详情
     */
    WorkflowInstanceVo instanceByBusinessId(String businessId, boolean administrator);

    /**
     * 根据业务 ID 查询流程轨迹。
     *
     * @param businessId 业务 ID
     * @param administrator 是否按管理员身份跳过参与人校验
     * @return 流程轨迹
     */
    List<WorkflowHistoryVo> instanceHistoryByBusinessId(
            String businessId, boolean administrator);

    /**
     * 根据业务 ID 查询脱敏后的流程图。
     *
     * @param businessId 业务 ID
     * @param administrator 是否按管理员身份跳过参与人校验
     * @return 流程图和轨迹
     */
    WorkflowInstanceGraphVo instanceGraphByBusinessId(
            String businessId, boolean administrator);

    /**
     * 查询过滤后的流程实例变量。
     *
     * @param instanceId 流程实例 ID
     * @return 可安全展示的流程变量
     */
    Map<String, Object> instanceVariables(Long instanceId);

    /**
     * 按流程白名单修改实例变量。
     *
     * @param instanceId 流程实例 ID
     * @param key 变量键
     * @param value 变量值
     * @param reason 修改原因
     * @return 操作结果
     */
    boolean updateInstanceVariable(Long instanceId, String key, Object value, String reason);

    /**
     * 删除未结束且未绑定业务的流程实例。
     *
     * @param instanceId 流程实例 ID
     * @return 操作结果
     */
    boolean deleteRunningInstance(Long instanceId);

    /**
     * 根据业务 ID 删除未结束且未绑定业务的流程实例。
     *
     * @param businessId 业务 ID
     * @return 操作结果
     */
    boolean deleteRunningInstanceByBusinessId(String businessId);

    /**
     * 删除已结束且未绑定业务的流程实例及历史。
     *
     * @param instanceId 流程实例 ID
     * @return 操作结果
     */
    boolean deleteFinishedInstanceHistory(Long instanceId);

    /**
     * 激活或挂起流程实例。
     *
     * @param instanceId 流程实例ID
     * @param active true激活，false挂起
     * @return 流程实例快照
     */
    com.vita.workflow.api.model.WorkflowInstanceSnapshot setInstanceActive(Long instanceId, boolean active);

    /**
     * 管理员作废流程实例。
     *
     * @param instanceId 流程实例ID
     * @param message 作废原因
     * @return 流程实例快照
     */
    com.vita.workflow.api.model.WorkflowInstanceSnapshot invalidateInstance(Long instanceId, String message);

    /**
     * 分页查询当前用户待办任务。
     *
     * @param searchDto 查询条件
     * @return 待办任务分页结果
     */
    PageResponse<WorkflowTaskVo> pendingTasks(WorkflowTaskSearchDto searchDto);

    /**
     * 分页查询当前用户已办任务。
     *
     * @param searchDto 查询条件
     * @return 已办任务分页结果
     */
    PageResponse<WorkflowTaskVo> completedTasks(WorkflowTaskSearchDto searchDto);

    /**
     * 管理端分页查询全部待办任务。
     *
     * @param searchDto 查询条件
     * @return 待办任务分页结果
     */
    PageResponse<WorkflowTaskVo> allPendingTasks(WorkflowTaskSearchDto searchDto);

    /**
     * 管理端分页查询全部已办任务。
     *
     * @param searchDto 查询条件
     * @return 已办任务分页结果
     */
    PageResponse<WorkflowTaskVo> allCompletedTasks(WorkflowTaskSearchDto searchDto);

    /**
     * 分页查询当前用户抄送记录。
     *
     * @param searchDto 查询条件
     * @return 抄送分页结果
     */
    PageResponse<WorkflowCopyVo> copiedTasks(WorkflowTaskSearchDto searchDto);

    /**
     * 查询当前用户可办理的任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    WorkflowTaskVo taskDetail(Long taskId);

    /**
     * 查询当前任务的全部候选办理标识。
     *
     * @param taskId 任务 ID
     * @return 办理人列表
     */
    List<WorkflowTaskHandlerVo> taskHandlers(Long taskId);

    /**
     * 管理员将当前任务办理人替换为启用用户。
     *
     * @param taskId 任务 ID
     * @param userIds 用户 ID
     * @param reason 修改原因
     * @return 操作结果
     */
    boolean updateTaskHandlers(Long taskId, List<Long> userIds, String reason);
}
