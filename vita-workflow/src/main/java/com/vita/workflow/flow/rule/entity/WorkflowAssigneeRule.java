package com.vita.workflow.flow.rule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule.entity
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则
 * @Version: 1.0
 */
@Data
@TableName("workflow_assignee_rule")
@EqualsAndHashCode(callSuper = true)
public class WorkflowAssigneeRule extends BaseEntity {

    private String ruleCode;

    private String ruleName;

    private String resolverCode;

    private String argumentNames;

    private Byte status;
}
