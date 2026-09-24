package com.vita.workflow.runtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.entity
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流任务抄送记录
 * @Version: 1.0
 */
@Data
@TableName("workflow_task_copy")
@EqualsAndHashCode(callSuper = true)
public class WorkflowTaskCopy extends BaseEntity {

    private Long instanceId;

    private Long historyTaskId;

    private String businessId;

    private Long recipientUserId;

    private String sourceNodeCode;

    private String sourceNodeName;

    private String message;

    private String attachmentRef;
}
