package com.vita.workflow.runtime.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 工作流定义展示对象
 * @Version: 1.0
 */
@Data
public class WorkflowDefinitionVo {

    private Long id;
    private String flowCode;
    private String flowName;
    private String category;
    private String categoryName;
    private String version;
    private Integer isPublish;
    private Integer activityStatus;
    private String formCustom;
    private String formPath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
