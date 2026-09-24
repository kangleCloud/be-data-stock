package com.vita.workflow.flow.rule.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule.vo
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则视图
 * @Version: 1.0
 */
@Data
public class WorkflowAssigneeRuleVo {

    private Long id;

    private String ruleCode;

    private String ruleName;

    private String resolverCode;

    private List<String> argumentNames = new ArrayList<>();

    private Byte status;

    private String expression;
}
