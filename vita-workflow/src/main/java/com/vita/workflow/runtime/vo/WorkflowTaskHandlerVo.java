package com.vita.workflow.runtime.vo;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.vo
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流当前任务办理人展示对象
 * @Version: 1.0
 */
@Data
public class WorkflowTaskHandlerVo {

    private String identifier;

    private String handlerType;

    private String handlerName;
}
