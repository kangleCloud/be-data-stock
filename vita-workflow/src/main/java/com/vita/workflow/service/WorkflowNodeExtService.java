package com.vita.workflow.service;

import com.vita.workflow.api.model.WorkflowTaskCapability;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import org.dromara.warm.flow.ui.service.NodeExtService;
import org.dromara.warm.flow.ui.vo.NodeExt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.service
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: Warm-Flow 设计器节点扩展配置
 * @Version: 1.0
 */
@Service
@ConditionalOnWorkflowEnabled
public class WorkflowNodeExtService implements NodeExtService {

    /**
     * 返回设计器使用的 Vita 节点扩展配置。
     *
     * @return 节点扩展配置
     */
    @Override
    public List<NodeExt> getNodeExt() {
        NodeExt nodeExt = new NodeExt();
        nodeExt.setCode("vita_workflow");
        nodeExt.setName("Vita工作流");
        nodeExt.setDesc("任务能力、抄送和动态选人配置");
        nodeExt.setType(2);
        nodeExt.setChilds(List.of(capabilityNode(), copyUserNode(), nextAssigneeNode()));
        return List.of(nodeExt);
    }

    private NodeExt.ChildNode capabilityNode() {
        NodeExt.ChildNode child = new NodeExt.ChildNode();
        child.setCode(WorkflowConstants.NODE_EXT_CAPABILITY);
        child.setLabel("任务能力");
        child.setDesc("新增高级操作必须显式勾选");
        child.setType(4);
        child.setMultiple(true);
        child.setDict(Arrays.stream(WorkflowTaskCapability.values())
                .filter(capability -> capability != WorkflowTaskCapability.SELECT_NEXT_ASSIGNEE)
                .map(capability -> new NodeExt.DictItem(
                        capability.name(), capability.name(), isLegacyCapability(capability)))
                .toList());
        return child;
    }

    private NodeExt.ChildNode copyUserNode() {
        NodeExt.ChildNode child = new NodeExt.ChildNode();
        child.setCode(WorkflowConstants.NODE_EXT_COPY_USER);
        child.setLabel("默认抄送用户");
        child.setDesc("仅支持系统用户");
        child.setType(5);
        child.setMultiple(true);
        return child;
    }

    private NodeExt.ChildNode nextAssigneeNode() {
        NodeExt.ChildNode child = new NodeExt.ChildNode();
        child.setCode(WorkflowConstants.NODE_EXT_NEXT_ASSIGNEE);
        child.setLabel("下一节点选人");
        child.setDesc("动态选人仅接受已启用用户");
        child.setType(3);
        child.setMultiple(false);
        child.setDict(List.of(
                new NodeExt.DictItem("使用定义候选人", WorkflowConstants.NEXT_ASSIGNEE_FIXED, true),
                new NodeExt.DictItem("办理时选择用户", WorkflowConstants.NEXT_ASSIGNEE_SELECT_USER, false)));
        return child;
    }

    private boolean isLegacyCapability(WorkflowTaskCapability capability) {
        return capability == WorkflowTaskCapability.PASS
                || capability == WorkflowTaskCapability.REJECT_LAST
                || capability == WorkflowTaskCapability.REJECT_TO;
    }
}
