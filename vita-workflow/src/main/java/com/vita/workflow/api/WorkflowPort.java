package com.vita.workflow.api;

import com.vita.workflow.api.command.*;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowStartResult;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 与具体工作流引擎无关的工作流操作端口
 * @Version: 1.0
 */
public interface WorkflowPort {

    /**
     * 创建流程实例。
     *
     * @param command 启动命令
     * @return 启动结果
     */
    WorkflowStartResult start(WorkflowStartCommand command);

    /**
     * 创建流程实例并提交首个任务。
     *
     * @param command 提交命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot startAndPass(WorkflowSubmitCommand command);

    /**
     * 通过当前任务。
     *
     * @param command 任务命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot pass(WorkflowTaskCommand command);

    /**
     * 退回上一人工节点。
     *
     * @param command 任务命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot rejectLast(WorkflowTaskCommand command);

    /**
     * 退回指定节点。
     *
     * @param command 退回命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot rejectTo(WorkflowRejectCommand command);

    /**
     * 在尚无审批人办理时撤回并终止流程实例。
     *
     * @param command 撤销命令
     * @return 实例快照
     */
    WorkflowInstanceSnapshot cancel(WorkflowCancelCommand command);

    /**
     * 根据全局业务 ID 查询实例。
     *
     * @param businessId 全局业务 ID
     * @return 实例快照
     */
    WorkflowInstanceSnapshot getByBusinessId(String businessId);

    /**
     * 判断用户是否为流程发起人或办理参与人。
     *
     * @param businessId 全局业务 ID
     * @param userId 用户 ID
     * @return true 有访问权限
     */
    boolean canAccess(String businessId, Long userId);
}
