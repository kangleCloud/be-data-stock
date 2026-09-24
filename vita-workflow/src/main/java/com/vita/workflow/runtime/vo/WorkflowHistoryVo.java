package com.vita.workflow.runtime.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流流转历史展示对象
 * @Version: 1.0
 */
@Data
public class WorkflowHistoryVo {

    private Long historyId;
    private Long taskId;
    private Long instanceId;
    private Integer nodeType;
    private String nodeCode;
    private String nodeName;
    private String targetNodeCode;
    private String targetNodeName;
    private String approverId;
    private String approverName;
    private String skipType;
    private String flowStatus;
    private String message;
    private String attachmentRef;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
