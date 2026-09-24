package com.vita.workflow.flow.rule.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.workflow.flow.rule.entity.WorkflowAssigneeRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule.mapper
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则 Mapper
 * @Version: 1.0
 */
@Mapper
public interface WorkflowAssigneeRuleMapper extends BaseMapperX<WorkflowAssigneeRule> {
}
