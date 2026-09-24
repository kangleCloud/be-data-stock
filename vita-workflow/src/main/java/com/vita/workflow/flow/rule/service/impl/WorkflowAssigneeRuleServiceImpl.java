package com.vita.workflow.flow.rule.service.impl;

import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.api.WorkflowAssigneeResolver;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleCreateDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleDeletedDto;
import com.vita.workflow.flow.rule.dto.WorkflowAssigneeRuleUpdateDto;
import com.vita.workflow.flow.rule.entity.WorkflowAssigneeRule;
import com.vita.workflow.flow.rule.mapper.WorkflowAssigneeRuleMapper;
import com.vita.workflow.flow.rule.service.WorkflowAssigneeRuleService;
import com.vita.workflow.flow.rule.support.WorkflowAssigneeRuleBridge;
import com.vita.workflow.flow.rule.vo.WorkflowAssigneeRuleVo;
import com.vita.workflow.runtime.mapper.WorkflowRuntimeQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule.service.impl
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则管理服务实现
 * @Version: 1.0
 */
@Service
@ConditionalOnWorkflowEnabled
public class WorkflowAssigneeRuleServiceImpl implements WorkflowAssigneeRuleService {

    private final WorkflowAssigneeRuleMapper ruleMapper;

    private final Map<String, WorkflowAssigneeResolver> resolverMap;

    private final WorkflowRuntimeQueryMapper workflowRuntimeQueryMapper;

    public WorkflowAssigneeRuleServiceImpl(
            WorkflowAssigneeRuleMapper ruleMapper,
            List<WorkflowAssigneeResolver> resolvers,
            WorkflowRuntimeQueryMapper workflowRuntimeQueryMapper) {
        this.ruleMapper = ruleMapper;
        this.workflowRuntimeQueryMapper = workflowRuntimeQueryMapper;
        this.resolverMap = new LinkedHashMap<>();
        resolvers.forEach(resolver -> {
            if (resolverMap.putIfAbsent(resolver.code(), resolver) != null) {
                throw new IllegalStateException("工作流办理人解析器编码重复: " + resolver.code());
            }
        });
    }

    /**
     * 查询规则列表。
     *
     * @param enabledOnly 是否只查询启用规则
     * @return 规则列表
     */
    @Override
    public List<WorkflowAssigneeRuleVo> list(boolean enabledOnly) {
        LambdaQueryWrapperX<WorkflowAssigneeRule> wrapper = new LambdaQueryWrapperX<>();
        if (enabledOnly) {
            wrapper.eq(WorkflowAssigneeRule::getStatus, CommonStatusEnum.ENABLED.getCode());
        }
        wrapper.orderByAsc(WorkflowAssigneeRule::getRuleCode, WorkflowAssigneeRule::getId);
        return ruleMapper.selectList(wrapper).stream().map(this::toVo).toList();
    }

    /**
     * 查询规则详情。
     *
     * @param ruleId 规则 ID
     * @return 规则详情
     */
    @Override
    public WorkflowAssigneeRuleVo detail(Long ruleId) {
        return toVo(requiredRule(ruleId));
    }

    /**
     * 创建规则。
     *
     * @param createDto 创建请求
     * @return 规则 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkflowAssigneeRuleCreateDto createDto) {
        if (ruleMapper.selectCount(new LambdaQueryWrapperX<WorkflowAssigneeRule>()
                .eq(WorkflowAssigneeRule::getRuleCode, createDto.getRuleCode())) > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "规则编码已存在");
        }
        validateResolver(createDto.getResolverCode());
        WorkflowAssigneeRule rule = new WorkflowAssigneeRule();
        rule.setRuleCode(createDto.getRuleCode());
        rule.setRuleName(createDto.getRuleName());
        rule.setResolverCode(createDto.getResolverCode());
        rule.setArgumentNames(joinArgumentNames(createDto.getArgumentNames()));
        rule.setStatus(createDto.getStatus());
        ruleMapper.insert(rule);
        return rule.getId();
    }

    /**
     * 更新规则，规则编码不允许修改。
     *
     * @param ruleId 规则 ID
     * @param updateDto 更新请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long ruleId, WorkflowAssigneeRuleUpdateDto updateDto) {
        WorkflowAssigneeRule rule = requiredRule(ruleId);
        validateResolver(updateDto.getResolverCode());
        String argumentNames = joinArgumentNames(updateDto.getArgumentNames());
        if (!Objects.equals(rule.getArgumentNames(), argumentNames)
                && isReferenced(rule)) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则已被流程定义引用，不能修改参数声明");
        }
        rule.setRuleName(updateDto.getRuleName());
        rule.setResolverCode(updateDto.getResolverCode());
        rule.setArgumentNames(argumentNames);
        rule.setStatus(updateDto.getStatus());
        ruleMapper.updateById(rule);
    }

    /**
     * 删除规则记录。已发布流程中的固定表达式会因规则缺失而拒绝解析。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(WorkflowAssigneeRuleDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        Long ruleId = deletedDto.getId();
        WorkflowAssigneeRule rule = requiredRule(ruleId);
        if (isReferenced(rule)) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则已被流程定义引用，不能删除");
        }
        ruleMapper.deleteById(ruleId);
    }

    private boolean isReferenced(WorkflowAssigneeRule rule) {
        String expression = WorkflowAssigneeRuleBridge.buildExpression(
                rule.getRuleCode(), splitArgumentNames(rule.getArgumentNames()));
        return workflowRuntimeQueryMapper.countNodesByPermissionExpression(expression) > 0;
    }

    private WorkflowAssigneeRule requiredRule(Long ruleId) {
        if (ruleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        WorkflowAssigneeRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return rule;
    }

    private void validateResolver(String resolverCode) {
        if (!resolverMap.containsKey(resolverCode)) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则解析器未注册");
        }
    }

    private String joinArgumentNames(List<String> argumentNames) {
        if (argumentNames == null || argumentNames.isEmpty()) {
            return "";
        }
        if (argumentNames.stream().anyMatch(name -> name == null || name.isBlank())) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则参数名不能为空或重复");
        }
        LinkedHashSet<String> normalizedNames = argumentNames.stream()
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (normalizedNames.size() != argumentNames.size()) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则参数名不能为空或重复");
        }
        return String.join(",", normalizedNames);
    }

    private List<String> splitArgumentNames(String argumentNames) {
        if (argumentNames == null || argumentNames.isBlank()) {
            return List.of();
        }
        return List.of(argumentNames.split(","));
    }

    private WorkflowAssigneeRuleVo toVo(WorkflowAssigneeRule rule) {
        WorkflowAssigneeRuleVo vo = new WorkflowAssigneeRuleVo();
        vo.setId(rule.getId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setResolverCode(rule.getResolverCode());
        vo.setArgumentNames(splitArgumentNames(rule.getArgumentNames()));
        vo.setStatus(rule.getStatus());
        vo.setExpression(WorkflowAssigneeRuleBridge.buildExpression(
                rule.getRuleCode(), vo.getArgumentNames()));
        return vo;
    }
}
