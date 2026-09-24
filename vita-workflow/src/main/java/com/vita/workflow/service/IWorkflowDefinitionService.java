package com.vita.workflow.service;

import org.dromara.warm.flow.core.dto.DefJson;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.service
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流定义导入服务
 * @Version: 1.0
 */
public interface IWorkflowDefinitionService {

    /**
     * 导入 Warm-Flow 流程定义 JSON。
     *
     * @param defJson 流程定义对象
     * @return 导入结果
     */
    boolean importJson(DefJson defJson);

}
