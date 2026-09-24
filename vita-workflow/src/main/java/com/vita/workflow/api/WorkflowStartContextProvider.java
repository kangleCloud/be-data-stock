package com.vita.workflow.api;

import com.vita.workflow.api.model.WorkflowStartContext;

import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: 按流程编码补充和校验启动上下文
 * @Version: 1.0
 */
public interface WorkflowStartContextProvider {

    /**
     * 判断是否处理指定流程。
     *
     * @param flowCode 流程编码
     * @return 是否支持
     */
    boolean supports(String flowCode);

    /**
     * 返回由服务端计算的流程变量。
     *
     * @param context 启动上下文
     * @return 服务端流程变量
     */
    Map<String, Object> provide(WorkflowStartContext context);
}
