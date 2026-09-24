package com.vita.workflow.runtime.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流实例展示对象
 * @Version: 1.0
 */
@Data
public class WorkflowInstanceVo {

    private Long instanceId;
    private Long definitionId;
    private String businessId;
    private String businessCode;
    private String businessTitle;
    private String flowCode;
    private String flowName;
    private String category;
    private String categoryName;
    private Integer nodeType;
    private String nodeCode;
    private String nodeName;
    private String flowStatus;
    private Integer activityStatus;
    private String initiatorId;
    private String initiatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
