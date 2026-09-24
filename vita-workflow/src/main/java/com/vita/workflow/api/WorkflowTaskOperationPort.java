package com.vita.workflow.api;

import com.vita.workflow.api.command.WorkflowNextNodeCommand;
import com.vita.workflow.api.command.WorkflowTaskOperationCommand;
import com.vita.workflow.api.model.WorkflowInstanceSnapshot;
import com.vita.workflow.api.model.WorkflowNodeSnapshot;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 与具体工作流引擎无关的高级任务操作端口
 * @Version: 1.0
 */
public interface WorkflowTaskOperationPort {

    /**
     * 预览任务办理后的下一节点。
     *
     * @param command 下一节点预览命令
     * @return 下一节点列表
     */
    List<WorkflowNodeSnapshot> previewNextNodes(WorkflowNextNodeCommand command);

    /**
     * 查询任务允许退回的节点。
     *
     * @param taskId 任务ID
     * @return 可退回节点列表
     */
    List<WorkflowNodeSnapshot> rejectableNodes(Long taskId);

    /**
     * 转办任务。
     *
     * @param command 高级任务操作命令
     * @return 流程实例快照
     */
    WorkflowInstanceSnapshot transfer(WorkflowTaskOperationCommand command);

    /**
     * 委派任务。
     *
     * @param command 高级任务操作命令
     * @return 流程实例快照
     */
    WorkflowInstanceSnapshot delegate(WorkflowTaskOperationCommand command);

    /**
     * 加签任务。
     *
     * @param command 高级任务操作命令
     * @return 流程实例快照
     */
    WorkflowInstanceSnapshot addSign(WorkflowTaskOperationCommand command);

    /**
     * 减签任务。
     *
     * @param command 高级任务操作命令
     * @return 流程实例快照
     */
    WorkflowInstanceSnapshot reduceSign(WorkflowTaskOperationCommand command);

    /**
     * 终止任务。
     *
     * @param command 高级任务操作命令
     * @return 流程实例快照
     */
    WorkflowInstanceSnapshot terminate(WorkflowTaskOperationCommand command);
}
