package com.vita.workflow.api;

import java.util.Set;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流实例变量写入策略
 * @Version: 1.0
 */
public interface WorkflowVariablePolicy {

    /**
     * 获取策略对应的流程编码。
     *
     * @return 流程编码
     */
    String flowCode();

    /**
     * 获取允许管理员修改的变量键。
     *
     * @return 可写变量键集合
     */
    Set<String> writableKeys();

    /**
     * 校验变量值。实现可以按业务规则抛出运行时异常拒绝写入。
     *
     * @param key 变量键
     * @param value 变量值
     */
    default void validate(String key, Object value) {
        // 默认仅执行键白名单校验，不限制具体值。
    }
}
