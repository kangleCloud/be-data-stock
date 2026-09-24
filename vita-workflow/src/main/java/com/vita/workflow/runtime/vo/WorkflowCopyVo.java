package com.vita.workflow.runtime.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流抄送展示对象
 * @Version: 1.0
 */
@Data
public class WorkflowCopyVo {

    private Long copyId;
    private Long instanceId;
    private Long historyTaskId;
    private String businessId;
    private String businessCode;
    private String businessTitle;
    private String flowCode;
    private String flowName;
    private String sourceNodeCode;
    private String sourceNodeName;
    private String message;
    private String attachmentRef;
    private LocalDateTime createTime;
}
