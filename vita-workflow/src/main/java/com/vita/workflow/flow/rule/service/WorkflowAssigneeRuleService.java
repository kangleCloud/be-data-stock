package com.vita.workflow.flow.rule.service;

import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleCreateDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleDeletedDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleUpdateDto;
import com.vita.workflow.flow.rule.vo.WorkflowAssigneeRuleVo;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule.service
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则管理服务
 * @Version: 1.0
 */
public interface WorkflowAssigneeRuleService {

    /**
     * 查询规则列表。
     *
     * @param enabledOnly 是否只查询启用规则
     * @return 规则列表
     */
    List<WorkflowAssigneeRuleVo> list(boolean enabledOnly);

    /**
     * 查询规则详情。
     *
     * @param ruleId 规则 ID
     * @return 规则详情
     */
    WorkflowAssigneeRuleVo detail(Long ruleId);

    /**
     * 创建规则。
     *
     * @param createDto 创建请求
     * @return 规则 ID
     */
    Long create(WorkflowAssigneeRuleCreateDto createDto);

    /**
     * 更新规则，规则编码保持不变。
     *
     * @param ruleId 规则 ID
     * @param updateDto 更新请求
     */
    void update(Long ruleId, WorkflowAssigneeRuleUpdateDto updateDto);

    /**
     * 删除规则。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(WorkflowAssigneeRuleDeletedDto deletedDto);
}
