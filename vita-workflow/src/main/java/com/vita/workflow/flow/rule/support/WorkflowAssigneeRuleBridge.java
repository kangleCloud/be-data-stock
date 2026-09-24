package com.vita.workflow.flow.rule.support;

import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.api.WorkflowAssigneeResolver;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.flow.rule.entity.WorkflowAssigneeRule;
import com.vita.workflow.flow.rule.mapper.WorkflowAssigneeRuleMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule.support
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则的固定表达式桥接组件
 * @Version: 1.0
 */
@Component("workflowAssigneeRuleBridge")
@ConditionalOnWorkflowEnabled
public class WorkflowAssigneeRuleBridge {

    private final WorkflowAssigneeRuleMapper ruleMapper;

    private final Map<String, WorkflowAssigneeResolver> resolverMap;

    public WorkflowAssigneeRuleBridge(
            WorkflowAssigneeRuleMapper ruleMapper,
            List<WorkflowAssigneeResolver> resolvers) {
        this.ruleMapper = ruleMapper;
        this.resolverMap = new LinkedHashMap<>();
        resolvers.forEach(resolver -> resolverMap.put(resolver.code(), resolver));
    }

    /**
     * 按规则编码和受控参数解析办理权限。
     *
     * @param ruleCode 规则编码
     * @param values 按规则参数声明排序的参数值
     * @return Warm-Flow 办理权限列表
     */
    public List<String> resolve(String ruleCode, Object... values) {
        WorkflowAssigneeRule rule = ruleMapper.selectOne(
                new LambdaQueryWrapperX<WorkflowAssigneeRule>()
                        .eq(WorkflowAssigneeRule::getRuleCode, ruleCode)
                        .eq(WorkflowAssigneeRule::getStatus, CommonStatusEnum.ENABLED.getCode()));
        if (rule == null) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则不存在或已停用");
        }
        WorkflowAssigneeResolver resolver = resolverMap.get(rule.getResolverCode());
        if (resolver == null) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则解析器未注册");
        }
        List<String> argumentNames = splitArgumentNames(rule.getArgumentNames());
        if (argumentNames.size() != values.length) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则参数数量不匹配");
        }
        Map<String, Object> arguments = new LinkedHashMap<>();
        for (int index = 0; index < argumentNames.size(); index++) {
            arguments.put(argumentNames.get(index), values[index]);
        }
        List<Long> userIds = resolver.resolve(arguments);
        if (userIds == null || userIds.isEmpty()) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则未解析到有效用户");
        }
        List<String> permissions = new LinkedHashSet<>(userIds).stream()
                .filter(userId -> userId != null)
                .map(WorkflowConstants::buildUserPermission)
                .toList();
        if (permissions.isEmpty()) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "办理人规则未解析到有效用户");
        }
        return permissions;
    }

    /**
     * 生成由平台控制的固定桥接表达式，不接受外部原始表达式。
     *
     * @param ruleCode 规则编码
     * @param argumentNames 参数名列表
     * @return Warm-Flow 表达式
     */
    public static String buildExpression(String ruleCode, List<String> argumentNames) {
        List<String> arguments = new ArrayList<>();
        arguments.add("'" + ruleCode + "'");
        if (argumentNames != null) {
            argumentNames.forEach(name -> arguments.add("#" + name));
        }
        return "#{@workflowAssigneeRuleBridge.resolve(" + String.join(",", arguments) + ")}";
    }

    private List<String> splitArgumentNames(String argumentNames) {
        if (argumentNames == null || argumentNames.isBlank()) {
            return List.of();
        }
        return List.of(argumentNames.split(","));
    }
}
