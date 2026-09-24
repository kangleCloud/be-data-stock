package com.vita.workflow.handler;

import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.support.WorkflowLoginUserSupport;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.handler
 * @Author: Codex
 * @CreateTime: 2026-07-02
 * @Description: Warm-Flow 办理人权限处理器
 * @Version: 1.0
 */
@Component
@ConditionalOnWorkflowEnabled
public class WorkflowPermissionHandler implements PermissionHandler {

    private final WorkflowLoginUserSupport workflowLoginUserSupport;

    public WorkflowPermissionHandler(WorkflowLoginUserSupport workflowLoginUserSupport) {
        this.workflowLoginUserSupport = workflowLoginUserSupport;
    }

    @Override
    public List<String> permissions() {
        return workflowLoginUserSupport.getCurrentPermissions();
    }

    /**
     * 返回当前办理人原始用户 ID，供历史办理记录留痕使用。
     */
    @Override
    public String getHandler() {
        return workflowLoginUserSupport.getCurrentHandler();
    }
}
