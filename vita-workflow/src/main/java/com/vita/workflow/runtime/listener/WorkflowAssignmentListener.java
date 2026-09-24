package com.vita.workflow.runtime.listener;

import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.listener
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 下一节点动态办理人分派监听器
 * @Version: 1.0
 */
@Component
@ConditionalOnWorkflowEnabled
public class WorkflowAssignmentListener implements GlobalListener {

    /**
     * 消费服务端生成的节点选人变量并覆盖对应下一任务的权限列表。
     *
     * @param listenerVariable Warm-Flow监听变量
     */
    @Override
    public void assignment(ListenerVariable listenerVariable) {
        Map<String, Object> variables = listenerVariable.getVariable();
        List<Task> nextTasks = listenerVariable.getNextTasks();
        if (variables == null || variables.isEmpty() || nextTasks == null || nextTasks.isEmpty()) {
            return;
        }
        for (Task nextTask : nextTasks) {
            String key = WorkflowConstants.INTERNAL_NEXT_ASSIGNEE_PREFIX + nextTask.getNodeCode();
            Object value = variables.remove(key);
            if (!(value instanceof Collection<?> collection)) {
                continue;
            }
            List<String> assignees = collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .distinct()
                    .toList();
            if (!assignees.isEmpty()) {
                nextTask.setPermissionList(assignees);
            }
        }
    }
}
