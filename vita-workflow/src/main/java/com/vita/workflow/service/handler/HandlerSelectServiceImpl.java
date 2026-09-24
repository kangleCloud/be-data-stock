package com.vita.workflow.service.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vita.auth.constant.AuthConstants;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.sysDept.entity.SysDept;
import com.vita.system.sysDept.service.ISysDeptService;
import com.vita.system.sysRole.entity.SysRole;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import com.vita.workflow.api.WorkflowHandlerSource;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.flow.rule.service.WorkflowAssigneeRuleService;
import com.vita.workflow.flow.rule.vo.WorkflowAssigneeRuleVo;
import jakarta.annotation.Resource;
import org.dromara.warm.flow.core.dto.Tree;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.dromara.warm.flow.ui.utils.TreeUtil;
import org.dromara.warm.flow.ui.vo.HandlerAuth;
import org.dromara.warm.flow.ui.vo.HandlerFeedBackVo;
import org.dromara.warm.flow.ui.vo.HandlerSelectVo;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.service.handler
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: Warm-Flow设计器办理人聚合选择器
 * @Version: 1.0
 */
@Component
@ConditionalOnWorkflowEnabled
public class HandlerSelectServiceImpl implements HandlerSelectService {

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final String ROLE_GROUP_NAME = "系统角色";

    private static final String DEPT_GROUP_NAME = "系统部门";

    private static final String NO_DEPT_GROUP_NAME = "未分配部门";

    private static final String RULE_GROUP_NAME = "受控规则";

    private final ISysRoleService sysRoleService;

    private final ISysDeptService sysDeptService;

    private final ISysUserService sysUserService;

    private final Map<String, java.util.function.Function<HandlerQuery, HandlerSelectVo>> handlerSources;

    @Resource
    private WorkflowAssigneeRuleService workflowAssigneeRuleService;

    public HandlerSelectServiceImpl(ISysRoleService sysRoleService,
                                    ISysDeptService sysDeptService,
                                    ISysUserService sysUserService,
                                    List<WorkflowHandlerSource> extensionSources) {
        this.sysRoleService = sysRoleService;
        this.sysDeptService = sysDeptService;
        this.sysUserService = sysUserService;
        this.handlerSources = new LinkedHashMap<>();
        handlerSources.put(WorkflowConstants.USER_HANDLER_TYPE, this::queryUsers);
        handlerSources.put(WorkflowConstants.ROLE_HANDLER_TYPE, this::queryRoles);
        handlerSources.put(WorkflowConstants.DEPT_HANDLER_TYPE, this::queryDepts);
        handlerSources.put(WorkflowConstants.RULE_HANDLER_TYPE, this::queryRules);
        registerExtensionSources(extensionSources);
    }

    /**
     * 返回支持的办理人类型列表。
     *
     * @return 办理人类型列表
     */
    @Override
    public List<String> getHandlerType() {
        return new ArrayList<>(handlerSources.keySet());
    }

    /**
     * 根据办理人类型查询办理人列表。
     *
     * @param query 查询参数
     * @return 办理人选择结果
     */
    @Override
    public HandlerSelectVo getHandlerSelect(HandlerQuery query) {
        String handlerType = resolveHandlerType(query);
        return handlerSources.getOrDefault(
                handlerType, handlerSources.get(WorkflowConstants.ROLE_HANDLER_TYPE)).apply(query);
    }

    /**
     * 注册业务模块提供的办理人来源。内置类型优先，重复编码直接拒绝启动。
     *
     * @param extensionSources 扩展办理人来源
     */
    private void registerExtensionSources(List<WorkflowHandlerSource> extensionSources) {
        if (extensionSources == null) {
            return;
        }
        for (WorkflowHandlerSource source : extensionSources) {
            if (source == null || CharSequenceUtil.isBlank(source.type())) {
                continue;
            }
            if (handlerSources.putIfAbsent(source.type(), query -> queryExtension(source, query)) != null) {
                throw new IllegalStateException("工作流办理人来源编码重复: " + source.type());
            }
        }
    }

    /**
     * 调用扩展来源并校验其返回值可供 Warm-Flow 设计器使用。
     *
     * @param source 扩展办理人来源
     * @param query Warm-Flow查询条件
     * @return 办理人选择结果
     */
    private HandlerSelectVo queryExtension(WorkflowHandlerSource source, HandlerQuery query) {
        Map<String, Object> sourceQuery = new LinkedHashMap<>();
        if (query != null) {
            sourceQuery.put("handlerType", query.getHandlerType());
            sourceQuery.put("handlerCode", query.getHandlerCode());
            sourceQuery.put("handlerName", query.getHandlerName());
            sourceQuery.put("groupId", query.getGroupId());
            sourceQuery.put("pageNum", query.getPageNum());
            sourceQuery.put("pageSize", query.getPageSize());
        }
        Object result = source.query(sourceQuery);
        if (result instanceof HandlerSelectVo handlerSelectVo) {
            return handlerSelectVo;
        }
        throw new IllegalStateException(
                "工作流办理人来源返回类型无效: " + source.type());
    }

    /**
     * 处理办理人反馈，根据入库主键集合查询对应的办理人名称。
     *
     * @param storageIds 入库主键集合
     * @return 办理人反馈列表
     */
    @Override
    public List<HandlerFeedBackVo> handlerFeedback(List<String> storageIds) {
        if (CollUtil.isEmpty(storageIds)) {
            return CollUtil.newArrayList();
        }

        Set<Long> userIds = new LinkedHashSet<>();
        Set<String> roleCodes = new LinkedHashSet<>();
        Set<Long> deptIds = new LinkedHashSet<>();
        for (String storageId : storageIds) {
            if (CharSequenceUtil.isBlank(storageId)) {
                continue;
            }
            if (storageId.startsWith(WorkflowConstants.USER_PERMISSION_PREFIX)) {
                Long userId = parseLong(removePrefix(storageId, WorkflowConstants.USER_PERMISSION_PREFIX));
                if (userId != null) {
                    userIds.add(userId);
                }
            } else if (storageId.startsWith(WorkflowConstants.ROLE_PERMISSION_PREFIX)) {
                String roleCode = removePrefix(storageId, WorkflowConstants.ROLE_PERMISSION_PREFIX);
                if (CharSequenceUtil.isNotBlank(roleCode)
                        && !AuthConstants.SUPER_ADMIN_ROLE_CODE.equals(roleCode)
                        && !WorkflowConstants.LEGACY_SUPER_ADMIN_ROLE_CODE.equals(roleCode)) {
                    roleCodes.add(roleCode);
                }
            } else if (storageId.startsWith(WorkflowConstants.DEPT_PERMISSION_PREFIX)) {
                Long deptId = parseLong(removePrefix(storageId, WorkflowConstants.DEPT_PERMISSION_PREFIX));
                if (deptId != null) {
                    deptIds.add(deptId);
                }
            }
        }

        Map<Long, String> userNameMap = queryUserNameMap(userIds);
        Map<String, String> roleNameMap = queryRoleNameMap(roleCodes);
        Map<Long, String> deptNameMap = queryDeptNameMap(deptIds);
        Map<String, String> ruleNameMap = queryRuleNameMap();

        return storageIds.stream()
                .map(storageId -> new HandlerFeedBackVo(storageId,
                        resolveFeedbackName(
                                storageId, userNameMap, roleNameMap, deptNameMap, ruleNameMap)))
                .toList();
    }

    /**
     * 查询平台注册的受控办理人规则。返回的存储值只能由平台固定模板生成，
     * 管理端不能向流程定义写入任意表达式。
     *
     * @param query 查询条件
     * @return 受控规则选择结果
     */
    private HandlerSelectVo queryRules(HandlerQuery query) {
        if (workflowAssigneeRuleService == null) {
            return getResult(List.of(), 0);
        }
        List<WorkflowAssigneeRuleVo> filtered = workflowAssigneeRuleService.list(true)
                .stream()
                .filter(rule -> matchesKeyword(
                        query == null ? null : query.getHandlerCode(), rule.getRuleCode()))
                .filter(rule -> matchesKeyword(
                        query == null ? null : query.getHandlerName(), rule.getRuleName()))
                .toList();
        int pageNum = resolvePageNum(query);
        int pageSize = resolvePageSize(query);
        int fromIndex = Math.min((pageNum - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<HandlerAuth> handlers = filtered.subList(fromIndex, toIndex)
                .stream()
                .map(rule -> new HandlerAuth()
                        .setStorageId(rule.getExpression())
                        .setHandlerCode(rule.getRuleCode())
                        .setHandlerName(rule.getRuleName())
                        .setGroupName(RULE_GROUP_NAME))
                .toList();
        return getResult(handlers, filtered.size());
    }

    /**
     * 查询角色列表
     *
     * @param query 查询条件
     * @return 角色列表
     */
    private HandlerSelectVo queryRoles(HandlerQuery query) {
        LambdaQueryWrapperX<SysRole> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(SysRole::getStatus, CommonStatusEnum.ENABLED.getCode());
        wrapper.neIfPresent(SysRole::getRoleCode, AuthConstants.SUPER_ADMIN_ROLE_CODE);
        wrapper.neIfPresent(SysRole::getRoleCode, WorkflowConstants.LEGACY_SUPER_ADMIN_ROLE_CODE);
        wrapper.likeIfPresent(SysRole::getRoleCode, query == null ? null : query.getHandlerCode());
        wrapper.likeIfPresent(SysRole::getRoleName, query == null ? null : query.getHandlerName());
        wrapper.orderByAsc(SysRole::getRoleSort, SysRole::getId);
        IPage<SysRole> page = sysRoleService.page(buildPage(query), wrapper);
        boolean includeVirtualSuperAdmin = shouldIncludeVirtualSuperAdmin(query);
        List<HandlerAuth> handlerAuths = new ArrayList<>();
        if (includeVirtualSuperAdmin) {
            handlerAuths.add(buildVirtualSuperAdminHandler());
        }
        handlerAuths.addAll(page.getRecords().stream()
                .map(role -> new HandlerAuth()
                        .setStorageId(WorkflowConstants.buildRolePermission(role.getRoleCode()))
                        .setHandlerCode(role.getRoleCode())
                        .setHandlerName(role.getRoleName())
                        .setGroupName(ROLE_GROUP_NAME))
                .toList());
        return getResult(handlerAuths, page.getTotal() + (includeVirtualSuperAdmin ? 1 : 0));
    }

    /**
     * 查询部门列表
     *
     * @param query 查询条件
     * @return 部门列表
     */
    private HandlerSelectVo queryDepts(HandlerQuery query) {
        LambdaQueryWrapperX<SysDept> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(SysDept::getStatus, CommonStatusEnum.ENABLED.getCode())
                .likeIfPresent(SysDept::getDeptCode, query == null ? null : query.getHandlerCode())
                .likeIfPresent(SysDept::getDeptName, query == null ? null : query.getHandlerName())
                .eqIfPresent(SysDept::getParentId, parseLong(query == null ? null : query.getGroupId()));
        wrapper.orderByAsc(SysDept::getSortNo, SysDept::getId);
        IPage<SysDept> page = sysDeptService.page(buildPage(query), wrapper);
        List<HandlerAuth> handlerAuths = page.getRecords().stream()
                .map(dept -> new HandlerAuth()
                        .setStorageId(WorkflowConstants.buildDeptPermission(dept.getId()))
                        .setHandlerCode(CharSequenceUtil.isNotBlank(dept.getDeptCode())
                                ? dept.getDeptCode()
                                : String.valueOf(dept.getId()))
                        .setHandlerName(dept.getDeptName())
                        .setGroupName(DEPT_GROUP_NAME))
                .toList();
        return getResult(handlerAuths, page.getTotal()).setTreeSelections(queryDeptTree());
    }

    /**
     * 查询用户列表
     *
     * @param query 查询条件
     * @return 用户列表
     */
    private HandlerSelectVo queryUsers(HandlerQuery query) {
        Map<Long, String> deptNameMap = queryDeptNameMap(null);
        LambdaQueryWrapperX<SysUser> wrapper = new LambdaQueryWrapperX<SysUser>()
                .eq(SysUser::getStatus, CommonStatusEnum.ENABLED.getCode())
                .eqIfPresent(SysUser::getDeptId, parseLong(query == null ? null : query.getGroupId()))
                .likeIfPresent(SysUser::getUserName, query == null ? null : query.getHandlerCode())
                .orderByDesc(SysUser::getId);
        if (query != null && CharSequenceUtil.isNotBlank(query.getHandlerName())) {
            wrapper.and(item -> item.like(SysUser::getNickName, query.getHandlerName())
                    .or()
                    .like(SysUser::getUserName, query.getHandlerName()));
        }
        IPage<SysUser> page = sysUserService.page(buildPage(query), wrapper);
        List<HandlerAuth> handlerAuths = page.getRecords().stream()
                .map(user -> new HandlerAuth()
                        .setStorageId(WorkflowConstants.buildUserPermission(user.getId()))
                        .setHandlerCode(user.getUserName())
                        .setHandlerName(resolveUserDisplayName(user))
                        .setGroupName(resolveUserGroupName(user, deptNameMap)))
                .toList();
        return getResult(handlerAuths, page.getTotal()).setTreeSelections(queryDeptTree());
    }

    private <T> Page<T> buildPage(HandlerQuery query) {
        return new Page<>(resolvePageNum(query), resolvePageSize(query));
    }

    private int resolvePageNum(HandlerQuery query) {
        if (query == null || query.getPageNum() == null || query.getPageNum() <= 0) {
            return DEFAULT_PAGE_NUM;
        }
        return query.getPageNum();
    }

    private int resolvePageSize(HandlerQuery query) {
        if (query == null || query.getPageSize() == null || query.getPageSize() <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return query.getPageSize();
    }

    /**
     * 解析办理人类型，如果为空，则默认返回角色类型。
     *
     * @param query 办理人查询对象
     * @return 办理人类型
     */
    private String resolveHandlerType(HandlerQuery query) {
        if (query == null || CharSequenceUtil.isBlank(query.getHandlerType())) {
            return WorkflowConstants.ROLE_HANDLER_TYPE;
        }
        return query.getHandlerType();
    }

    private List<Tree> queryDeptTree() {
        LambdaQueryWrapperX<SysDept> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(SysDept::getStatus, CommonStatusEnum.ENABLED.getCode());
        queryWrapper.orderByAsc(SysDept::getSortNo, SysDept::getId);
        List<Tree> deptTrees = sysDeptService.list(queryWrapper)
                .stream()
                .map(dept -> new Tree(String.valueOf(dept.getId()),
                        dept.getDeptName(),
                        dept.getParentId() == null ? null : String.valueOf(dept.getParentId()),
                        null))
                .toList();
        return TreeUtil.buildTree(deptTrees);
    }

    private Map<Long, String> queryUserNameMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserService.list(new LambdaQueryWrapperX<SysUser>().in(SysUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, this::resolveUserDisplayName, (left, right) -> left));
    }

    private Map<String, String> queryRoleNameMap(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Map.of();
        }
        return sysRoleService.list(new LambdaQueryWrapperX<SysRole>().in(SysRole::getRoleCode, roleCodes))
                .stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, SysRole::getRoleName, (left, right) -> left));
    }

    /**
     * 查询部门名称映射表
     *
     * @param deptIds 部门ID集合，如果为空，则查询所有部门
     * @return 部门ID与部门名称的映射表
     */
    private Map<Long, String> queryDeptNameMap(Set<Long> deptIds) {
        LambdaQueryWrapperX<SysDept> wrapper = new LambdaQueryWrapperX<>();
        if (deptIds != null) {
            if (deptIds.isEmpty()) {
                return MapUtil.empty();
            }
            wrapper.in(SysDept::getId, deptIds);
        }
        return sysDeptService.list(wrapper)
                .stream()
                // 如果存在重复的部门ID，则保留第一个部门名称
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (left, right) -> left));
    }

    /**
     * 解析办理人反馈名称
     *
     * @param storageId   存储ID
     * @param userNameMap 用户名称映射表
     * @param roleNameMap 角色名称映射表
     * @param deptNameMap 部门名称映射表
     * @return 办理人反馈名称
     */
    private String resolveFeedbackName(String storageId,
                                       Map<Long, String> userNameMap,
                                       Map<String, String> roleNameMap,
                                       Map<Long, String> deptNameMap,
                                       Map<String, String> ruleNameMap) {
        if (CharSequenceUtil.isBlank(storageId)) {
            return "";
        }
        if (storageId.startsWith(WorkflowConstants.USER_PERMISSION_PREFIX)) {
            Long userId = parseLong(removePrefix(storageId, WorkflowConstants.USER_PERMISSION_PREFIX));
            return userId == null ? "" : userNameMap.getOrDefault(userId, "");
        }
        if (storageId.startsWith(WorkflowConstants.ROLE_PERMISSION_PREFIX)) {
            String roleCode = removePrefix(storageId, WorkflowConstants.ROLE_PERMISSION_PREFIX);
            if (AuthConstants.SUPER_ADMIN_ROLE_CODE.equals(roleCode)
                    || WorkflowConstants.LEGACY_SUPER_ADMIN_ROLE_CODE.equals(roleCode)) {
                return WorkflowConstants.SUPER_ADMIN_HANDLER_NAME;
            }
            return roleNameMap.getOrDefault(roleCode, "");
        }
        if (storageId.startsWith(WorkflowConstants.DEPT_PERMISSION_PREFIX)) {
            Long deptId = parseLong(removePrefix(storageId, WorkflowConstants.DEPT_PERMISSION_PREFIX));
            return deptId == null ? "" : deptNameMap.getOrDefault(deptId, "");
        }
        return ruleNameMap.getOrDefault(storageId, "");
    }

    private Map<String, String> queryRuleNameMap() {
        if (workflowAssigneeRuleService == null) {
            return Map.of();
        }
        return workflowAssigneeRuleService.list(false)
                .stream()
                .collect(Collectors.toMap(
                        WorkflowAssigneeRuleVo::getExpression,
                        WorkflowAssigneeRuleVo::getRuleName,
                        (left, right) -> left));
    }

    /**
     * 解析用户显示名称，如果昵称不为空，则返回昵称，否则返回用户名。
     *
     * @param user 用户对象
     * @return 用户显示名称
     */
    private String resolveUserDisplayName(SysUser user) {
        if (CharSequenceUtil.isNotBlank(user.getNickName())) {
            return user.getNickName();
        }
        return user.getUserName();
    }

    /**
     * 解析用户组名称
     *
     * @param user        用户对象
     * @param deptNameMap 部门名称映射表
     * @return 用户组名称
     */
    private String resolveUserGroupName(SysUser user, Map<Long, String> deptNameMap) {
        if (user.getDeptId() == null) {
            return NO_DEPT_GROUP_NAME;
        }
        return deptNameMap.getOrDefault(user.getDeptId(), NO_DEPT_GROUP_NAME);
    }

    /**
     * 移除前缀
     *
     * @param value  原始值
     * @param prefix 前缀
     * @return 移除前缀后的值
     */
    private String removePrefix(String value, String prefix) {
        return value.substring(prefix.length());
    }

    /**
     * 解析字符串为 Long 类型，如果解析失败，则返回 null。
     *
     * @param value 字符串值
     * @return Long 类型值，解析失败则返回 null
     */
    private Long parseLong(String value) {
        if (CharSequenceUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean shouldIncludeVirtualSuperAdmin(HandlerQuery query) {
        return matchesKeyword(query == null ? null : query.getHandlerCode(), AuthConstants.SUPER_ADMIN_ROLE_CODE)
                && matchesKeyword(query == null ? null : query.getHandlerName(), WorkflowConstants.SUPER_ADMIN_HANDLER_NAME);
    }

    private boolean matchesKeyword(String keyword, String target) {
        return CharSequenceUtil.isBlank(keyword) || CharSequenceUtil.containsIgnoreCase(target, keyword.trim());
    }

    private HandlerAuth buildVirtualSuperAdminHandler() {
        return new HandlerAuth()
                .setStorageId(WorkflowConstants.buildRolePermission(AuthConstants.SUPER_ADMIN_ROLE_CODE))
                .setHandlerCode(AuthConstants.SUPER_ADMIN_ROLE_CODE)
                .setHandlerName(WorkflowConstants.SUPER_ADMIN_HANDLER_NAME)
                .setGroupName(ROLE_GROUP_NAME);
    }
}
