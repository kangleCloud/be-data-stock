package com.vita.workflow.controller.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 下一节点预览请求
 * @Version: 1.0
 */
@Data
public class WorkflowNextNodeRequest {

    private Map<String, Object> variables = new HashMap<>();
}
