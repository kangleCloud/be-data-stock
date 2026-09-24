package com.vita.workflow.api;

import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则解析器
 * @Version: 1.0
 */
public interface WorkflowAssigneeResolver {

    /**
     * 获取解析器编码。
     *
     * @return 解析器编码
     */
    String code();

    /**
     * 根据受控参数解析用户ID。
     *
     * @param arguments 规则参数
     * @return 用户ID列表
     */
    List<Long> resolve(Map<String, Object> arguments);
}
