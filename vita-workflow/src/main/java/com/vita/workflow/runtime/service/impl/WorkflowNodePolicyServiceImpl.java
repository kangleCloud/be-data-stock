package com.vita.workflow.runtime.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.vita.workflow.api.model.WorkflowTaskCapability;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.runtime.model.WorkflowNodePolicy;
import com.vita.workflow.runtime.service.WorkflowNodePolicyService;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.service.NodeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.service.impl
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流节点扩展策略默认实现
 * @Version: 1.0
 */
@Service
@ConditionalOnWorkflowEnabled
public class WorkflowNodePolicyServiceImpl implements WorkflowNodePolicyService {

    private final NodeService nodeService;

    public WorkflowNodePolicyServiceImpl(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    /**
     * 解析指定节点的任务能力、默认抄送人和动态选人策略。
     *
     * @param definitionId 流程定义ID
     * @param nodeCode 节点编码
     * @return 节点运行时策略
     */
    @Override
    public WorkflowNodePolicy getPolicy(Long definitionId, String nodeCode) {
        if (definitionId == null || !StringUtils.hasText(nodeCode)) {
            return WorkflowNodePolicy.legacy();
        }
        Node node = nodeService.getByDefIdAndNodeCode(definitionId, nodeCode);
        if (node == null || !StringUtils.hasText(node.getExt())) {
            return WorkflowNodePolicy.legacy();
        }
        JSONArray extensions;
        try {
            extensions = JSON.parseArray(node.getExt());
        } catch (Exception ex) {
            return WorkflowNodePolicy.legacy();
        }
        JSONObject capabilityExtension = findExtension(extensions, WorkflowConstants.NODE_EXT_CAPABILITY);
        if (capabilityExtension == null) {
            return WorkflowNodePolicy.legacy();
        }
        Set<WorkflowTaskCapability> capabilities = parseCapabilities(capabilityExtension.getString("value"));
        Set<Long> copyUsers = parseCopyUsers(
                valueOf(extensions, WorkflowConstants.NODE_EXT_COPY_USER));
        boolean selectAssignee = WorkflowConstants.NEXT_ASSIGNEE_SELECT_USER.equalsIgnoreCase(
                valueOf(extensions, WorkflowConstants.NODE_EXT_NEXT_ASSIGNEE));
        if (selectAssignee) {
            capabilities.add(WorkflowTaskCapability.SELECT_NEXT_ASSIGNEE);
        }
        return new WorkflowNodePolicy(
                Collections.unmodifiableSet(capabilities),
                Collections.unmodifiableSet(copyUsers),
                selectAssignee);
    }

    private JSONObject findExtension(JSONArray extensions, String code) {
        if (extensions == null) {
            return null;
        }
        return extensions.stream()
                .filter(JSONObject.class::isInstance)
                .map(JSONObject.class::cast)
                .filter(item -> code.equals(item.getString("code")))
                .findFirst()
                .orElse(null);
    }

    private String valueOf(JSONArray extensions, String code) {
        JSONObject extension = findExtension(extensions, code);
        return extension == null ? null : extension.getString("value");
    }

    private Set<WorkflowTaskCapability> parseCapabilities(String value) {
        Set<WorkflowTaskCapability> capabilities = new LinkedHashSet<>();
        if (!StringUtils.hasText(value)) {
            return capabilities;
        }
        for (String item : value.split(",")) {
            try {
                capabilities.add(WorkflowTaskCapability.valueOf(item.trim()));
            } catch (IllegalArgumentException ignored) {
                // 未知能力由服务端忽略，避免旧配置阻断整个任务详情。
            }
        }
        return capabilities;
    }

    private Set<Long> parseCopyUsers(String value) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (!StringUtils.hasText(value)) {
            return userIds;
        }
        for (String item : value.split(",")) {
            String userId = item.trim();
            if (userId.startsWith(WorkflowConstants.USER_PERMISSION_PREFIX)) {
                userId = userId.substring(WorkflowConstants.USER_PERMISSION_PREFIX.length());
            }
            try {
                userIds.add(Long.valueOf(userId));
            } catch (NumberFormatException ignored) {
                // 仅接受用户ID，角色、部门和表达式不会进入运行时抄送列表。
            }
        }
        return userIds;
    }
}
