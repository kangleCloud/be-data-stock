package com.vita.workflow.runtime.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流任务展示对象
 * @Version: 1.0
 */
@Data
public class WorkflowTaskVo {

    private Long taskId;
    private Long historyId;
    private Long instanceId;
    private Long definitionId;
    private String businessId;
    private String businessCode;
    private String businessTitle;
    private String flowCode;
    private String flowName;
    private String nodeCode;
    private String nodeName;
    private String flowStatus;
    private String formCustom;
    private String formPath;
    private String approverId;
    private String approverName;
    private String message;
    private String allowedRejectNodeCode;
    private Set<String> allowedOperations = new LinkedHashSet<>();
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
