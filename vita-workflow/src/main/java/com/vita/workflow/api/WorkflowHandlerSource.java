package com.vita.workflow.api;

import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流办理人来源扩展协议
 * @Version: 1.0
 */
public interface WorkflowHandlerSource {

    /**
     * 获取办理人来源编码。
     *
     * @return 来源编码
     */
    String type();

    /**
     * 查询办理人来源数据。
     *
     * @param query 查询参数
     * @return 来源数据
     */
    Object query(Map<String, Object> query);
}
