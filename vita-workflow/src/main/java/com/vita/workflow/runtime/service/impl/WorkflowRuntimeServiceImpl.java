package com.vita.workflow.runtime.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import com.vita.workflow.api.WorkflowStartContextProvider;
import com.vita.workflow.api.WorkflowVariablePolicy;
import com.vita.workflow.api.command.*;
import com.vita.workflow.api.event.WorkflowChangedEvent;
import com.vita.workflow.api.event.WorkflowOperation;
import com.vita.workflow.api.model.*;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.reimbursement.entity.WorkflowReimbursement;
import com.vita.workflow.reimbursement.mapper.WorkflowReimbursementMapper;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.entity.WorkflowInstanceMetadata;
import com.vita.workflow.runtime.entity.WorkflowTaskCopy;
import com.vita.workflow.runtime.mapper.WorkflowInstanceMetadataMapper;
import com.vita.workflow.runtime.mapper.WorkflowRuntimeQueryMapper;
import com.vita.workflow.runtime.mapper.WorkflowTaskCopyMapper;
import com.vita.workflow.runtime.model.WorkflowNodePolicy;
import com.vita.workflow.runtime.service.WorkflowNodePolicyService;
import com.vita.workflow.runtime.service.WorkflowRuntimeService;
import com.vita.workflow.runtime.support.WorkflowAfterCommitEventPublisher;
import com.vita.workflow.runtime.support.WorkflowDistributedLock;
import com.vita.workflow.runtime.support.WorkflowEngineExceptionTranslator;
import com.vita.workflow.runtime.vo.*;
import com.vita.workflow.support.WorkflowLoginUserSupport;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.*;
import org.dromara.warm.flow.core.enums.*;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.dromara.warm.flow.ui.vo.HandlerFeedBackVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.service.impl
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 基于 Warm-Flow 的通用工作流运行时实现
 * @Version: 1.0
 */
@Service
@ConditionalOnWorkflowEnabled
public class WorkflowRuntimeServiceImpl implements WorkflowRuntimeService {

    private static final String GRAPH_STATE_CURRENT = "CURRENT";

    private static final String GRAPH_STATE_COMPLETED = "COMPLETED";

    private static final String GRAPH_STATE_PENDING = "PENDING";

    private final DefService defService;
    private final InsService insService;
    private final TaskService taskService;
    private final NodeService nodeService;
    private final SkipService skipService;
    private final UserService userService;
    private final HandlerSelectService handlerSelectService;
    private final WorkflowRuntimeQueryMapper queryMapper;
    private final WorkflowInstanceMetadataMapper metadataMapper;
    private final WorkflowTaskCopyMapper taskCopyMapper;
    private final WorkflowReimbursementMapper reimbursementMapper;
    private final WorkflowNodePolicyService nodePolicyService;
    private final ISysUserService sysUserService;
    private final WorkflowLoginUserSupport loginUserSupport;
    private final List<WorkflowStartContextProvider> startContextProviders;
    private final Map<String, WorkflowVariablePolicy> variablePolicies;
    private final WorkflowDistributedLock distributedLock;
    private final WorkflowAfterCommitEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public WorkflowRuntimeServiceImpl(DefService defService,
                                      InsService insService,
                                      TaskService taskService,
                                      NodeService nodeService,
                                      SkipService skipService,
                                      UserService userService,
                                      HandlerSelectService handlerSelectService,
                                      WorkflowRuntimeQueryMapper queryMapper,
                                      WorkflowInstanceMetadataMapper metadataMapper,
                                      WorkflowTaskCopyMapper taskCopyMapper,
                                      WorkflowReimbursementMapper reimbursementMapper,
                                      WorkflowNodePolicyService nodePolicyService,
                                      ISysUserService sysUserService,
                                      WorkflowLoginUserSupport loginUserSupport,
                                      List<WorkflowStartContextProvider> startContextProviders,
                                      List<WorkflowVariablePolicy> variablePolicies,
                                      WorkflowDistributedLock distributedLock,
                                      WorkflowAfterCommitEventPublisher eventPublisher,
                                      PlatformTransactionManager transactionManager) {
        this.defService = defService;
        this.insService = insService;
        this.taskService = taskService;
        this.nodeService = nodeService;
        this.skipService = skipService;
        this.userService = userService;
        this.handlerSelectService = handlerSelectService;
        this.queryMapper = queryMapper;
        this.metadataMapper = metadataMapper;
        this.taskCopyMapper = taskCopyMapper;
        this.reimbursementMapper = reimbursementMapper;
        this.nodePolicyService = nodePolicyService;
        this.sysUserService = sysUserService;
        this.loginUserSupport = loginUserSupport;
        this.startContextProviders = startContextProviders;
        this.variablePolicies = buildVariablePolicies(variablePolicies);
        this.distributedLock = distributedLock;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public WorkflowStartResult start(WorkflowStartCommand command) {
        validateStartCommand(command);
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "start:" + command.getBusinessId();
        return distributedLock.execute(lockKey,
                () -> transactionTemplate.execute(status -> doStart(command)));
    }

    @Override
    public WorkflowInstanceSnapshot startAndPass(WorkflowSubmitCommand command) {
        validateSubmitCommand(command);
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "start:" + command.getBusinessId();
        // 实例创建和首任务办理共享业务锁与事务，任一步失败都不得留下半提交实例。
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowStartCommand startCommand = new WorkflowStartCommand();
            startCommand.setFlowCode(command.getFlowCode());
            startCommand.setBusinessId(command.getBusinessId());
            startCommand.setVariables(safeVariables(command.getVariables()));
            startCommand.setMetadata(command.getMetadata());
            WorkflowStartResult startResult = doStart(startCommand);

            WorkflowTaskVo task = requireTask(startResult.getTaskId());
            requireTaskAccess(task.getTaskId());
            requireCapability(task, WorkflowTaskCapability.PASS);
            Map<String, Object> variables = prepareTaskVariables(
                    task, command.getVariables(), Map.of());
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.skip(task.getTaskId(), FlowParams.build()
                            .skipType(SkipType.PASS.getKey())
                            .message(command.getMessage())
                            .variable(variables)
                            .hisTaskExt(command.getAttachmentRef())));
            saveTaskCopies(task, command.getCopyUserIds(), command.getMessage(), command.getAttachmentRef());
            return buildOperationResult(task, instance, WorkflowOperation.PASS, task.getTaskId());
        }));
    }

    private WorkflowStartResult doStart(WorkflowStartCommand command) {
        if (queryMapper.selectInstanceByBusinessId(command.getBusinessId()) != null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务ID已存在，不能重复发起流程");
        }
        Definition definition = requirePublishedDefinition(command.getFlowCode());
        Long operatorId = currentUserId();
        Long deptId = loginUserSupport.getCurrentDeptId();
        Map<String, Object> variables = command.getVariables() == null
                ? new HashMap<>() : new HashMap<>(command.getVariables());
        variables.entrySet().removeIf(entry -> entry.getValue() == null);
        // 平台身份变量覆盖客户端同名值，流程 Provider 变量最后合并并拥有最高优先级。
        variables.put(WorkflowConstants.INITIATOR_ID_VARIABLE, operatorId);
        if (deptId != null) {
            variables.put(WorkflowConstants.INITIATOR_DEPT_ID_VARIABLE, deptId);
        }
        WorkflowStartContext context = new WorkflowStartContext(
                command.getFlowCode(), command.getBusinessId(), operatorId, deptId, Map.copyOf(variables));
        startContextProviders.stream()
                .filter(provider -> provider.supports(command.getFlowCode()))
                .map(provider -> provider.provide(context))
                .filter(Objects::nonNull)
                .forEach(variables::putAll);

        FlowParams flowParams = FlowParams.build()
                .flowCode(command.getFlowCode())
                .handler(String.valueOf(operatorId))
                .variable(variables)
                .flowStatus(FlowStatus.TOBESUBMIT.getKey());
        Instance instance = WorkflowEngineExceptionTranslator.execute(
                () -> insService.start(command.getBusinessId(), flowParams));
        saveInstanceMetadata(command.getMetadata(), definition, instance);
        Long taskId = queryMapper.selectCurrentTaskId(instance.getId());
        if (taskId == null) {
            throw new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), "流程启动失败，未生成首个任务");
        }
        publishEvent(WorkflowOperation.START, definition, instance, taskId, operatorId,
                null, instance.getNodeCode());
        return toStartResult(definition, instance, taskId);
    }

    @Override
    public WorkflowInstanceSnapshot pass(WorkflowTaskCommand command) {
        validateTaskCommand(command);
        return executeTask(command.getTaskId(), task -> {
            requireCapability(task, WorkflowTaskCapability.PASS);
            Map<String, Object> variables = prepareTaskVariables(
                    task, command.getVariables(), command.getNextAssignees());
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.skip(command.getTaskId(), FlowParams.build()
                            .skipType(SkipType.PASS.getKey())
                            .message(command.getMessage())
                            .variable(variables)
                            .hisTaskExt(command.getAttachmentRef())));
            saveTaskCopies(task, command.getCopyUserIds(), command.getMessage(), command.getAttachmentRef());
            return buildOperationResult(task, instance, WorkflowOperation.PASS, command.getTaskId());
        });
    }

    @Override
    public WorkflowInstanceSnapshot rejectLast(WorkflowTaskCommand command) {
        validateTaskCommand(command);
        return executeTask(command.getTaskId(), task -> {
            requireCapability(task, WorkflowTaskCapability.REJECT_LAST);
            boolean hasPreviousUserTask = queryMapper.selectHistoryByInstanceId(task.getInstanceId()).stream()
                    .anyMatch(history -> Integer.valueOf(1).equals(history.getNodeType())
                            && targetContains(history.getTargetNodeCode(), task.getNodeCode()));
            if (!hasPreviousUserTask) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "当前任务不存在可退回的上一人工节点");
            }
            FlowParams flowParams = FlowParams.build()
                    .message(command.getMessage())
                    .variable(safeTaskVariables(command.getVariables()))
                    .hisTaskExt(command.getAttachmentRef());
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.rejectLast(command.getTaskId(), flowParams));
            saveTaskCopies(task, command.getCopyUserIds(), command.getMessage(), command.getAttachmentRef());
            return buildOperationResult(task, instance, WorkflowOperation.REJECT_LAST, command.getTaskId());
        });
    }

    @Override
    public WorkflowInstanceSnapshot rejectTo(WorkflowRejectCommand command) {
        validateRejectCommand(command);
        return executeTask(command.getTaskId(), task -> {
            requireCapability(task, WorkflowTaskCapability.REJECT_TO);
            if (!targetContains(task.getAllowedRejectNodeCode(), command.getTargetNodeCode())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "目标节点不在当前流程定义允许的退回范围内");
            }
            FlowParams flowParams = FlowParams.build()
                    .nodeCode(command.getTargetNodeCode())
                    .skipType(SkipType.REJECT.getKey())
                    .message(command.getMessage())
                    .variable(safeTaskVariables(command.getVariables()))
                    .hisTaskExt(command.getAttachmentRef());
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.skip(command.getTaskId(), flowParams));
            saveTaskCopies(
                    task, command.getCopyUserIds(), command.getMessage(), command.getAttachmentRef());
            return buildOperationResult(task, instance, WorkflowOperation.REJECT_TO, command.getTaskId());
        });
    }

    @Override
    public WorkflowInstanceSnapshot cancel(WorkflowCancelCommand command) {
        if (command == null || command.getInstanceId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程实例ID不能为空");
        }
        validateMessageAndAttachment(command.getMessage(), null);
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "instance:" + command.getInstanceId();
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowInstanceVo current = requireInstance(command.getInstanceId());
            Long operatorId = currentUserId();
            if (!String.valueOf(operatorId).equals(current.getInitiatorId())) {
                throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "只有流程发起人可以撤销流程");
            }
            if (Integer.valueOf(2).equals(current.getNodeType())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "已结束流程不能撤销");
            }
            List<WorkflowHistoryVo> userTaskHistory = queryMapper.selectHistoryByInstanceId(command.getInstanceId())
                    .stream()
                    .filter(history -> Integer.valueOf(WorkflowConstants.USER_TASK_NODE_TYPE)
                            .equals(history.getNodeType()))
                    .toList();
            // 只允许“申请人已提交但尚无其他人工节点办理”的实例进入撤回终态。
            if (userTaskHistory.size() > 1
                    || userTaskHistory.stream().anyMatch(history ->
                    !String.valueOf(operatorId).equals(history.getApproverId()))) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程已有审批人办理，不能撤回");
            }
            Long taskId = queryMapper.selectCurrentTaskId(command.getInstanceId());
            FlowParams flowParams = FlowParams.build()
                    .handler(String.valueOf(operatorId))
                    .message(command.getMessage())
                    .flowStatus(FlowStatus.CANCEL.getKey())
                    .hisStatus(FlowStatus.CANCEL.getKey())
                    .ignore(true);
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.terminationByInsId(command.getInstanceId(), flowParams));
            Definition definition = requireDefinition(instance.getDefinitionId());
            publishEvent(WorkflowOperation.CANCEL, definition, instance, taskId, operatorId,
                    current.getNodeCode(), instance.getNodeCode());
            return toSnapshot(definition, instance, null);
        }));
    }

    @Override
    public WorkflowInstanceSnapshot getByBusinessId(String businessId) {
        if (!StringUtils.hasText(businessId)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务ID不能为空");
        }
        WorkflowInstanceVo instance = queryMapper.selectInstanceByBusinessId(businessId);
        if (instance == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
        }
        return toSnapshot(instance, queryMapper.selectCurrentTaskId(instance.getInstanceId()));
    }

    @Override
    public boolean canAccess(String businessId, Long userId) {
        if (!StringUtils.hasText(businessId) || userId == null) {
            return false;
        }
        Long currentUserId = currentUserId();
        if (!userId.equals(currentUserId)) {
            return false;
        }
        return queryMapper.countInstanceAccessByBusinessId(
                businessId, String.valueOf(userId), currentPermissions()) > 0;
    }

    @Override
    public PageResponse<WorkflowDefinitionVo> publishedDefinitions(WorkflowDefinitionSearchDto searchDto) {
        WorkflowDefinitionSearchDto query = searchDto == null ? new WorkflowDefinitionSearchDto() : searchDto;
        IPage<WorkflowDefinitionVo> page = queryMapper.selectDefinitionPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, true);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<WorkflowDefinitionVo> definitionPage(WorkflowDefinitionSearchDto searchDto) {
        WorkflowDefinitionSearchDto query = searchDto == null ? new WorkflowDefinitionSearchDto() : searchDto;
        IPage<WorkflowDefinitionVo> page = queryMapper.selectDefinitionPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, false);
        return toPageResponse(page);
    }

    @Override
    public WorkflowDefinitionVo definitionDetail(Long definitionId) {
        WorkflowDefinitionVo definition = queryMapper.selectDefinitionById(definitionId);
        if (definition == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程定义不存在");
        }
        return definition;
    }

    @Override
    public boolean publishDefinition(Long definitionId) {
        requireDefinition(definitionId);
        return WorkflowEngineExceptionTranslator.execute(() -> defService.publish(definitionId));
    }

    @Override
    public boolean unpublishDefinition(Long definitionId) {
        requireDefinition(definitionId);
        return WorkflowEngineExceptionTranslator.execute(() -> defService.unPublish(definitionId));
    }

    @Override
    public boolean copyDefinition(Long definitionId) {
        requireDefinition(definitionId);
        return WorkflowEngineExceptionTranslator.execute(() -> defService.copyDef(definitionId));
    }

    @Override
    public String exportDefinitionJson(Long definitionId) {
        requireDefinition(definitionId);
        return WorkflowEngineExceptionTranslator.execute(() -> defService.exportJson(definitionId));
    }

    @Override
    public boolean setDefinitionActive(Long definitionId, boolean active) {
        requireDefinition(definitionId);
        return WorkflowEngineExceptionTranslator.execute(
                () -> active ? defService.active(definitionId) : defService.unActive(definitionId));
    }

    @Override
    public void deleteDefinition(Long definitionId) {
        requireDefinition(definitionId);
        if (queryMapper.countInstancesByDefinitionId(definitionId) > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程定义已产生实例，不能删除");
        }
        WorkflowEngineExceptionTranslator.execute(() -> defService.removeDef(List.of(definitionId)));
    }

    @Override
    public PageResponse<WorkflowInstanceVo> myInstances(WorkflowInstanceSearchDto searchDto) {
        WorkflowInstanceSearchDto query = searchDto == null ? new WorkflowInstanceSearchDto() : searchDto;
        IPage<WorkflowInstanceVo> page = queryMapper.selectInstancePage(
                new Page<>(query.getPageNum(), query.getPageSize()), query,
                String.valueOf(currentUserId()), null);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<WorkflowInstanceVo> runningInstances(WorkflowInstanceSearchDto searchDto) {
        return instancePage(searchDto, false);
    }

    @Override
    public PageResponse<WorkflowInstanceVo> finishedInstances(WorkflowInstanceSearchDto searchDto) {
        return instancePage(searchDto, true);
    }

    private PageResponse<WorkflowInstanceVo> instancePage(WorkflowInstanceSearchDto searchDto, boolean finished) {
        WorkflowInstanceSearchDto query = searchDto == null ? new WorkflowInstanceSearchDto() : searchDto;
        IPage<WorkflowInstanceVo> page = queryMapper.selectInstancePage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, null, finished);
        return toPageResponse(page);
    }

    @Override
    public WorkflowInstanceVo instanceDetail(Long instanceId, boolean administrator) {
        WorkflowInstanceVo instance = requireInstance(instanceId);
        if (!administrator) {
            requireInstanceAccess(instanceId);
        }
        return instance;
    }

    @Override
    public List<WorkflowHistoryVo> instanceHistory(Long instanceId, boolean administrator) {
        requireInstance(instanceId);
        if (!administrator) {
            requireInstanceAccess(instanceId);
        }
        return queryMapper.selectHistoryByInstanceId(instanceId);
    }

    /**
     * 根据业务 ID 查询实例，并在普通访问场景执行参与人校验。
     *
     * @param businessId 业务 ID
     * @param administrator 是否管理员访问
     * @return 流程实例
     */
    @Override
    public WorkflowInstanceVo instanceByBusinessId(String businessId, boolean administrator) {
        WorkflowInstanceVo instance = requireInstanceByBusinessId(businessId);
        if (!administrator) {
            requireInstanceAccess(instance.getInstanceId());
        }
        return instance;
    }

    /**
     * 根据业务 ID 查询流程轨迹，访问规则与实例详情保持一致。
     *
     * @param businessId 业务 ID
     * @param administrator 是否管理员访问
     * @return 流程轨迹
     */
    @Override
    public List<WorkflowHistoryVo> instanceHistoryByBusinessId(
            String businessId, boolean administrator) {
        WorkflowInstanceVo instance = instanceByBusinessId(businessId, administrator);
        return queryMapper.selectHistoryByInstanceId(instance.getInstanceId());
    }

    /**
     * 构建不包含办理表达式、跳转条件和流程变量的流程图安全投影。
     *
     * @param businessId 业务 ID
     * @param administrator 是否管理员访问
     * @return 流程图
     */
    @Override
    public WorkflowInstanceGraphVo instanceGraphByBusinessId(
            String businessId, boolean administrator) {
        WorkflowInstanceVo instance = instanceByBusinessId(businessId, administrator);
        List<WorkflowHistoryVo> history =
                queryMapper.selectHistoryByInstanceId(instance.getInstanceId());
        List<Task> currentTasks = WorkflowEngineExceptionTranslator.execute(
                () -> taskService.getByInsId(instance.getInstanceId()));
        Set<String> currentNodeCodes = currentTasks == null
                ? Set.of()
                : currentTasks.stream()
                .map(Task::getNodeCode)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> completedNodeCodes = history.stream()
                .map(WorkflowHistoryVo::getNodeCode)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (Integer.valueOf(2).equals(instance.getNodeType())
                && StringUtils.hasText(instance.getNodeCode())) {
            completedNodeCodes.add(instance.getNodeCode());
        }

        List<Node> nodes = WorkflowEngineExceptionTranslator.execute(
                () -> nodeService.getByDefId(instance.getDefinitionId()));
        List<Skip> skips = WorkflowEngineExceptionTranslator.execute(
                () -> skipService.getByDefId(instance.getDefinitionId()));
        WorkflowInstanceGraphVo graph = new WorkflowInstanceGraphVo();
        graph.setInstance(instance);
        graph.setHistory(history);
        graph.setNodes(nodes == null ? List.of() : nodes.stream()
                .map(node -> toGraphNode(node, currentNodeCodes, completedNodeCodes))
                .toList());
        Set<String> completedEdges = completedGraphEdges(history);
        graph.setEdges(skips == null ? List.of() : skips.stream()
                .map(skip -> toGraphEdge(skip, completedEdges))
                .toList());
        return graph;
    }

    /**
     * 查询实例变量时统一移除平台身份、候选人和内部动态选人变量。
     *
     * @param instanceId 流程实例 ID
     * @return 可展示变量
     */
    @Override
    public Map<String, Object> instanceVariables(Long instanceId) {
        requireInstance(instanceId);
        Instance instance = WorkflowEngineExceptionTranslator.execute(
                () -> insService.getById(instanceId));
        if (instance == null || instance.getVariableMap() == null) {
            return Map.of();
        }
        Map<String, Object> result = new TreeMap<>(instance.getVariableMap());
        result.keySet().removeIf(this::isProtectedVariable);
        return Collections.unmodifiableMap(result);
    }

    /**
     * 在实例锁内按流程白名单修改变量，避免与任务办理并发覆盖变量快照。
     *
     * @param instanceId 流程实例 ID
     * @param key 变量键
     * @param value 变量值
     * @param reason 修改原因
     * @return 操作结果
     */
    @Override
    public boolean updateInstanceVariable(
            Long instanceId, String key, Object value, String reason) {
        if (instanceId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程实例ID不能为空");
        }
        validateVariableUpdate(key, value, reason);
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "instance:" + instanceId;
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowInstanceVo current = requireInstance(instanceId);
            if (Integer.valueOf(2).equals(current.getNodeType())) {
                throw new ServiceException(
                        GlobalErrorCode.BAD_REQUEST.getCode(), "已结束流程不能修改变量");
            }
            WorkflowVariablePolicy policy = variablePolicies.get(current.getFlowCode());
            if (policy == null || policy.writableKeys() == null
                    || !policy.writableKeys().contains(key)) {
                throw new ServiceException(
                        GlobalErrorCode.FORBIDDEN.getCode(), "当前流程未开放该变量的修改权限");
            }
            policy.validate(key, value);
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> insService.getById(instanceId));
            if (instance == null) {
                throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
            }
            // 使用引擎合并变量并持久化，禁止通过自定义 Mapper 直接修改 flow_instance。
            taskService.mergeVariable(instance, Map.of(key, value));
            boolean updated = WorkflowEngineExceptionTranslator.execute(
                    () -> insService.updateById(instance));
            if (!updated) {
                throw new ServiceException(
                        GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), "流程变量修改失败");
            }
            Definition definition = requireDefinition(instance.getDefinitionId());
            publishEvent(WorkflowOperation.VARIABLE_UPDATE, definition, instance,
                    queryMapper.selectCurrentTaskId(instanceId), currentUserId(),
                    current.getNodeCode(), current.getNodeCode());
            return true;
        }));
    }

    /**
     * 删除未结束且无业务绑定的流程实例。
     *
     * @param instanceId 流程实例 ID
     * @return 操作结果
     */
    @Override
    public boolean deleteRunningInstance(Long instanceId) {
        return deleteUnboundInstance(instanceId, false);
    }

    /**
     * 根据业务 ID 删除未结束且无业务绑定的流程实例。
     *
     * @param businessId 业务 ID
     * @return 操作结果
     */
    @Override
    public boolean deleteRunningInstanceByBusinessId(String businessId) {
        return deleteRunningInstance(requireInstanceByBusinessId(businessId).getInstanceId());
    }

    /**
     * 删除已结束且无业务绑定的流程实例及其历史记录。
     *
     * @param instanceId 流程实例 ID
     * @return 操作结果
     */
    @Override
    public boolean deleteFinishedInstanceHistory(Long instanceId) {
        return deleteUnboundInstance(instanceId, true);
    }

    @Override
    public WorkflowInstanceSnapshot setInstanceActive(Long instanceId, boolean active) {
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "instance:" + instanceId;
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowInstanceVo current = requireInstance(instanceId);
            if (Integer.valueOf(2).equals(current.getNodeType())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "已结束流程不能切换激活状态");
            }
            WorkflowEngineExceptionTranslator.execute(
                    () -> active ? insService.active(instanceId) : insService.unActive(instanceId));
            Instance instance = WorkflowEngineExceptionTranslator.execute(() -> insService.getById(instanceId));
            Definition definition = requireDefinition(instance.getDefinitionId());
            publishEvent(active ? WorkflowOperation.ACTIVATE : WorkflowOperation.SUSPEND,
                    definition, instance, queryMapper.selectCurrentTaskId(instanceId), currentUserId(),
                    current.getNodeCode(), current.getNodeCode());
            return toSnapshot(definition, instance, queryMapper.selectCurrentTaskId(instanceId));
        }));
    }

    @Override
    public WorkflowInstanceSnapshot invalidateInstance(Long instanceId, String message) {
        validateMessageAndAttachment(message, null);
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "instance:" + instanceId;
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowInstanceVo current = requireInstance(instanceId);
            if (Integer.valueOf(2).equals(current.getNodeType())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "已结束流程不能作废");
            }
            Long taskId = queryMapper.selectCurrentTaskId(instanceId);
            FlowParams flowParams = FlowParams.build()
                    .handler(String.valueOf(currentUserId()))
                    .message(message)
                    .flowStatus(FlowStatus.NULLIFY.getKey())
                    .hisStatus(FlowStatus.NULLIFY.getKey())
                    .ignore(true);
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.terminationByInsId(instanceId, flowParams));
            Definition definition = requireDefinition(instance.getDefinitionId());
            publishEvent(WorkflowOperation.INVALIDATE, definition, instance, taskId, currentUserId(),
                    current.getNodeCode(), instance.getNodeCode());
            return toSnapshot(definition, instance, null);
        }));
    }

    @Override
    public PageResponse<WorkflowTaskVo> pendingTasks(WorkflowTaskSearchDto searchDto) {
        WorkflowTaskSearchDto query = searchDto == null ? new WorkflowTaskSearchDto() : searchDto;
        IPage<WorkflowTaskVo> page = queryMapper.selectPendingTaskPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, currentPermissions());
        return toPageResponse(page);
    }

    @Override
    public PageResponse<WorkflowTaskVo> completedTasks(WorkflowTaskSearchDto searchDto) {
        WorkflowTaskSearchDto query = searchDto == null ? new WorkflowTaskSearchDto() : searchDto;
        IPage<WorkflowTaskVo> page = queryMapper.selectCompletedTaskPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query,
                String.valueOf(currentUserId()));
        return toPageResponse(page);
    }

    @Override
    public PageResponse<WorkflowTaskVo> allPendingTasks(WorkflowTaskSearchDto searchDto) {
        WorkflowTaskSearchDto query = searchDto == null ? new WorkflowTaskSearchDto() : searchDto;
        return toPageResponse(queryMapper.selectAllPendingTaskPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query));
    }

    @Override
    public PageResponse<WorkflowTaskVo> allCompletedTasks(WorkflowTaskSearchDto searchDto) {
        WorkflowTaskSearchDto query = searchDto == null ? new WorkflowTaskSearchDto() : searchDto;
        return toPageResponse(queryMapper.selectAllCompletedTaskPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query));
    }

    @Override
    public PageResponse<WorkflowCopyVo> copiedTasks(WorkflowTaskSearchDto searchDto) {
        WorkflowTaskSearchDto query = searchDto == null ? new WorkflowTaskSearchDto() : searchDto;
        IPage<WorkflowCopyVo> page = queryMapper.selectCopiedTaskPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, currentUserId());
        return new PageResponse<>(page.getRecords(), page.getTotal());
    }

    @Override
    public WorkflowTaskVo taskDetail(Long taskId) {
        WorkflowTaskVo task = requireTask(taskId);
        requireTaskAccess(taskId);
        enrichTaskCapabilities(task);
        return task;
    }

    /**
     * 查询当前任务的候选办理标识，名称回显复用设计器办理人解析能力。
     *
     * @param taskId 任务 ID
     * @return 办理人列表
     */
    @Override
    public List<WorkflowTaskHandlerVo> taskHandlers(Long taskId) {
        requireTask(taskId);
        requireTaskAccess(taskId);
        List<String> permissions = activeTaskPermissions(taskId);
        List<String> feedbackIds = permissions.stream()
                .map(this::toFeedbackIdentifier)
                .toList();
        List<HandlerFeedBackVo> feedback = handlerSelectService.handlerFeedback(feedbackIds);
        Map<String, String> names = feedback == null
                ? Map.of()
                : feedback.stream().collect(java.util.stream.Collectors.toMap(
                HandlerFeedBackVo::getStorageId,
                item -> Objects.toString(item.getHandlerName(), ""),
                (left, right) -> left,
                LinkedHashMap::new));
        List<WorkflowTaskHandlerVo> result = new ArrayList<>(permissions.size());
        for (int index = 0; index < permissions.size(); index++) {
            String permission = permissions.get(index);
            String feedbackId = feedbackIds.get(index);
            WorkflowTaskHandlerVo handler = new WorkflowTaskHandlerVo();
            handler.setIdentifier(permission);
            handler.setHandlerType(resolveHandlerType(permission));
            handler.setHandlerName(names.getOrDefault(feedbackId, ""));
            result.add(handler);
        }
        return result;
    }

    /**
     * 管理员在任务锁内将全部候选标识替换为启用用户 ID。
     *
     * @param taskId 任务 ID
     * @param userIds 用户 ID
     * @param reason 修改原因
     * @return 操作结果
     */
    @Override
    public boolean updateTaskHandlers(Long taskId, List<Long> userIds, String reason) {
        if (taskId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID不能为空");
        }
        validateMessageAndAttachment(reason, null);
        if (!StringUtils.hasText(reason)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "修改原因不能为空");
        }
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "task:" + taskId;
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowTaskVo task = requireTask(taskId);
            // 用户启用状态必须在锁内复核，避免校验后到引擎写入前发生状态变化。
            List<String> targetUsers = validateEnabledUsers(userIds).stream()
                    .map(String::valueOf)
                    .toList();
            List<String> currentHandlers = activeTaskPermissions(taskId);
            if (new LinkedHashSet<>(currentHandlers).equals(new LinkedHashSet<>(targetUsers))) {
                return true;
            }
            FlowParams flowParams = FlowParams.build()
                    .handler(String.valueOf(currentUserId()))
                    .addHandlers(targetUsers)
                    .reductionHandlers(currentHandlers)
                    .message(reason)
                    .ignore(true);
            boolean updated = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.updateHandler(taskId, flowParams));
            if (!updated) {
                throw new ServiceException(
                        GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), "任务办理人修改失败");
            }
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> insService.getById(task.getInstanceId()));
            Definition definition = requireDefinition(task.getDefinitionId());
            publishEvent(WorkflowOperation.MODIFY_HANDLER, definition, instance,
                    taskId, currentUserId(), task.getNodeCode(), task.getNodeCode());
            return true;
        }));
    }

    @Override
    public List<WorkflowNodeSnapshot> previewNextNodes(WorkflowNextNodeCommand command) {
        if (command == null || command.getTaskId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID不能为空");
        }
        WorkflowTaskVo task = requireTask(command.getTaskId());
        requireTaskAccess(command.getTaskId());
        return previewNextNodes(task, command.getVariables());
    }

    @Override
    public List<WorkflowNodeSnapshot> rejectableNodes(Long taskId) {
        WorkflowTaskVo task = requireTask(taskId);
        requireTaskAccess(taskId);
        requireCapability(task, WorkflowTaskCapability.REJECT_TO);
        LinkedHashSet<String> nodeCodes = new LinkedHashSet<>();
        if (StringUtils.hasText(task.getAllowedRejectNodeCode())) {
            Arrays.stream(task.getAllowedRejectNodeCode().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(nodeCodes::add);
        }
        if (nodeCodes.isEmpty()) {
            return List.of();
        }
        return nodeService.getByNodeCodes(new ArrayList<>(nodeCodes), task.getDefinitionId()).stream()
                .map(node -> toNodeSnapshot(task.getDefinitionId(), node))
                .toList();
    }

    @Override
    public WorkflowInstanceSnapshot transfer(WorkflowTaskOperationCommand command) {
        return collaborate(command, WorkflowTaskCapability.TRANSFER,
                WorkflowOperation.TRANSFER, CooperateType.TRANSFER, true);
    }

    @Override
    public WorkflowInstanceSnapshot delegate(WorkflowTaskOperationCommand command) {
        return collaborate(command, WorkflowTaskCapability.DELEGATE,
                WorkflowOperation.DELEGATE, CooperateType.DEPUTE, true);
    }

    @Override
    public WorkflowInstanceSnapshot addSign(WorkflowTaskOperationCommand command) {
        return collaborate(command, WorkflowTaskCapability.ADD_SIGN,
                WorkflowOperation.ADD_SIGN, CooperateType.ADD_SIGNATURE, false);
    }

    @Override
    public WorkflowInstanceSnapshot reduceSign(WorkflowTaskOperationCommand command) {
        validateOperationCommand(command, false);
        return executeTask(command.getTaskId(), task -> {
            requireCapability(task, WorkflowTaskCapability.REDUCE_SIGN);
            List<String> requested = validateEnabledUsers(command.getUserIds()).stream()
                    .map(String::valueOf)
                    .toList();
            List<String> currentPermissions = activeTaskPermissions(task.getTaskId());
            if (requested.stream().anyMatch(permission -> !currentPermissions.contains(permission))) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "只能减签运行期新增的用户");
            }
            if (currentPermissions.size() - requested.size() < 1) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "减签后必须保留至少一个办理人");
            }
            FlowParams flowParams = FlowParams.build()
                    .handler(String.valueOf(currentUserId()))
                    .permissionFlag(currentPermissions())
                    .reductionHandlers(requested)
                    .cooperateType(CooperateType.REDUCTION_SIGNATURE.getKey())
                    .message(command.getMessage())
                    .hisTaskExt(command.getAttachmentRef());
            WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.updateHandler(task.getTaskId(), flowParams));
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> insService.getById(task.getInstanceId()));
            return buildOperationResult(task, instance, WorkflowOperation.REDUCE_SIGN, task.getTaskId());
        });
    }

    @Override
    public WorkflowInstanceSnapshot terminate(WorkflowTaskOperationCommand command) {
        validateOperationCommand(command, true);
        return executeTask(command.getTaskId(), task -> {
            requireCapability(task, WorkflowTaskCapability.TERMINATE);
            FlowParams flowParams = FlowParams.build()
                    .handler(String.valueOf(currentUserId()))
                    .permissionFlag(currentPermissions())
                    .message(command.getMessage())
                    .hisTaskExt(command.getAttachmentRef())
                    .flowStatus(FlowStatus.TERMINATE.getKey())
                    .hisStatus(FlowStatus.TERMINATE.getKey());
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.termination(task.getTaskId(), flowParams));
            return buildOperationResult(task, instance, WorkflowOperation.TERMINATE, task.getTaskId());
        });
    }

    private WorkflowInstanceSnapshot collaborate(WorkflowTaskOperationCommand command,
                                                 WorkflowTaskCapability capability,
                                                 WorkflowOperation operation,
                                                 CooperateType cooperateType,
                                                 boolean replaceExisting) {
        validateOperationCommand(command, false);
        return executeTask(command.getTaskId(), task -> {
            requireCapability(task, capability);
            List<String> targetUsers = validateEnabledUsers(command.getUserIds()).stream()
                    .map(String::valueOf)
                    .toList();
            if (replaceExisting && targetUsers.size() != 1) {
                throw new ServiceException(
                        GlobalErrorCode.BAD_REQUEST.getCode(), "转办和委派只能选择一个目标用户");
            }
            List<String> currentPermissions = activeTaskPermissions(task.getTaskId());
            if (targetUsers.stream().anyMatch(currentPermissions::contains)) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "目标用户已是当前任务办理人");
            }
            FlowParams flowParams = FlowParams.build()
                    .handler(String.valueOf(currentUserId()))
                    .permissionFlag(currentPermissions())
                    .addHandlers(targetUsers)
                    .cooperateType(cooperateType.getKey())
                    .message(command.getMessage())
                    .hisTaskExt(command.getAttachmentRef());
            if (replaceExisting) {
                flowParams.reductionHandlers(currentPermissions);
            }
            WorkflowEngineExceptionTranslator.execute(
                    () -> taskService.updateHandler(task.getTaskId(), flowParams));
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> insService.getById(task.getInstanceId()));
            return buildOperationResult(task, instance, operation, task.getTaskId());
        });
    }

    /**
     * 删除实例前在同一个实例锁和事务内校验运行状态与业务绑定。
     *
     * @param instanceId 流程实例 ID
     * @param finished true 仅允许删除已结束实例，false 仅允许删除运行实例
     * @return 操作结果
     */
    private boolean deleteUnboundInstance(Long instanceId, boolean finished) {
        if (instanceId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程实例ID不能为空");
        }
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "instance:" + instanceId;
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowInstanceVo current = requireInstance(instanceId);
            boolean currentFinished = Integer.valueOf(2).equals(current.getNodeType());
            if (finished != currentFinished) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        finished ? "只能删除已结束流程历史" : "已结束流程只能通过历史删除接口清理");
            }
            requireNoBusinessBinding(instanceId);
            Instance instance = WorkflowEngineExceptionTranslator.execute(
                    () -> insService.getById(instanceId));
            if (instance == null) {
                throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
            }
            Definition definition = requireDefinition(instance.getDefinitionId());
            Long taskId = queryMapper.selectCurrentTaskId(instanceId);
            // InsService.remove 负责清理待办、历史任务和 flow_user，禁止直接写引擎表。
            boolean deleted = WorkflowEngineExceptionTranslator.execute(
                    () -> insService.remove(List.of(instanceId)));
            if (!deleted) {
                throw new ServiceException(
                        GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), "流程实例删除失败");
            }
            taskCopyMapper.delete(new LambdaQueryWrapperX<WorkflowTaskCopy>()
                    .eq(WorkflowTaskCopy::getInstanceId, instanceId));
            publishEvent(finished ? WorkflowOperation.DELETE_HISTORY : WorkflowOperation.DELETE,
                    definition, instance, taskId, currentUserId(),
                    current.getNodeCode(), current.getNodeCode());
            return true;
        }));
    }

    /**
     * 业务元数据或报销记录存在时禁止物理删除，统一保留审计链并使用作废。
     *
     * @param instanceId 流程实例 ID
     */
    private void requireNoBusinessBinding(Long instanceId) {
        long metadataCount = metadataMapper.selectCount(
                new LambdaQueryWrapperX<WorkflowInstanceMetadata>()
                        .eq(WorkflowInstanceMetadata::getInstanceId, instanceId));
        long reimbursementCount = reimbursementMapper.selectCount(
                new LambdaQueryWrapperX<WorkflowReimbursement>()
                        .eq(WorkflowReimbursement::getInstanceId, instanceId));
        if (metadataCount > 0 || reimbursementCount > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "流程实例已绑定业务数据，请使用作废操作保留审计历史");
        }
    }

    private WorkflowGraphNodeVo toGraphNode(
            Node node, Set<String> currentNodeCodes, Set<String> completedNodeCodes) {
        WorkflowGraphNodeVo result = new WorkflowGraphNodeVo();
        result.setNodeCode(node.getNodeCode());
        result.setNodeName(node.getNodeName());
        result.setNodeType(node.getNodeType());
        result.setCoordinate(node.getCoordinate());
        if (currentNodeCodes.contains(node.getNodeCode())) {
            result.setState(GRAPH_STATE_CURRENT);
        } else if (completedNodeCodes.contains(node.getNodeCode())) {
            result.setState(GRAPH_STATE_COMPLETED);
        } else {
            result.setState(GRAPH_STATE_PENDING);
        }
        return result;
    }

    private WorkflowGraphEdgeVo toGraphEdge(Skip skip, Set<String> completedEdges) {
        WorkflowGraphEdgeVo result = new WorkflowGraphEdgeVo();
        result.setSourceNodeCode(skip.getNowNodeCode());
        result.setTargetNodeCode(skip.getNextNodeCode());
        result.setCoordinate(skip.getCoordinate());
        String edgeKey = graphEdgeKey(skip.getNowNodeCode(), skip.getNextNodeCode());
        result.setState(completedEdges.contains(edgeKey)
                ? GRAPH_STATE_COMPLETED : GRAPH_STATE_PENDING);
        return result;
    }

    private Set<String> completedGraphEdges(List<WorkflowHistoryVo> history) {
        Set<String> completedEdges = new LinkedHashSet<>();
        for (WorkflowHistoryVo item : history) {
            if (!StringUtils.hasText(item.getNodeCode())
                    || !StringUtils.hasText(item.getTargetNodeCode())) {
                continue;
            }
            Arrays.stream(item.getTargetNodeCode().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(target -> graphEdgeKey(item.getNodeCode(), target))
                    .forEach(completedEdges::add);
        }
        return completedEdges;
    }

    private String graphEdgeKey(String sourceNodeCode, String targetNodeCode) {
        return sourceNodeCode + "->" + targetNodeCode;
    }

    private Map<String, WorkflowVariablePolicy> buildVariablePolicies(
            List<WorkflowVariablePolicy> policies) {
        Map<String, WorkflowVariablePolicy> result = new LinkedHashMap<>();
        if (policies == null) {
            return Map.of();
        }
        for (WorkflowVariablePolicy policy : policies) {
            if (policy == null || !StringUtils.hasText(policy.flowCode())) {
                throw new IllegalStateException("工作流变量策略必须声明流程编码");
            }
            if (result.putIfAbsent(policy.flowCode(), policy) != null) {
                throw new IllegalStateException(
                        "工作流变量策略流程编码重复: " + policy.flowCode());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private void validateVariableUpdate(String key, Object value, String reason) {
        if (!StringUtils.hasText(key) || key.length() > 100 || !key.equals(key.trim())) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(),
                    "变量键不能为空、首尾不能含空格且长度不能超过100");
        }
        if (value == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "变量值不能为空");
        }
        if (!StringUtils.hasText(reason) || reason.length() > 500) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "修改原因不能为空且长度不能超过500");
        }
        if (isProtectedVariable(key)) {
            throw new ServiceException(
                    GlobalErrorCode.FORBIDDEN.getCode(), "平台保护变量不能读取或修改");
        }
    }

    private boolean isProtectedVariable(String key) {
        return key == null
                || WorkflowConstants.PROTECTED_TASK_VARIABLES.contains(key)
                || key.startsWith(WorkflowConstants.INTERNAL_NEXT_ASSIGNEE_PREFIX)
                || key.startsWith("_vita");
    }

    private String toFeedbackIdentifier(String permission) {
        return isRawUserId(permission)
                ? WorkflowConstants.buildUserPermission(Long.valueOf(permission))
                : permission;
    }

    private String resolveHandlerType(String permission) {
        if (isRawUserId(permission)
                || permission.startsWith(WorkflowConstants.USER_PERMISSION_PREFIX)) {
            return "USER";
        }
        if (permission.startsWith(WorkflowConstants.ROLE_PERMISSION_PREFIX)) {
            return "ROLE";
        }
        if (permission.startsWith(WorkflowConstants.DEPT_PERMISSION_PREFIX)) {
            return "DEPT";
        }
        return "RULE";
    }

    private void saveInstanceMetadata(WorkflowBusinessMetadata metadata,
                                      Definition definition,
                                      Instance instance) {
        WorkflowInstanceMetadata entity = new WorkflowInstanceMetadata();
        entity.setBusinessId(instance.getBusinessId());
        entity.setInstanceId(instance.getId());
        entity.setFlowCode(definition.getFlowCode());
        entity.setBusinessCode(metadata == null || !StringUtils.hasText(metadata.getBusinessCode())
                ? definition.getFlowCode() : metadata.getBusinessCode().trim());
        entity.setBusinessTitle(metadata == null || !StringUtils.hasText(metadata.getBusinessTitle())
                ? instance.getBusinessId() : metadata.getBusinessTitle().trim());
        metadataMapper.insert(entity);
    }

    /**
     * 在引擎完成任务并生成历史记录后保存抄送快照，抄送记录不参与任务候选人计算。
     */
    private void saveTaskCopies(WorkflowTaskVo task,
                                List<Long> requestedUserIds,
                                String message,
                                String attachmentRef) {
        WorkflowNodePolicy policy = nodePolicyService.getPolicy(task.getDefinitionId(), task.getNodeCode());
        Set<Long> copyUserIds = new LinkedHashSet<>(policy.defaultCopyUserIds());
        if (requestedUserIds != null && !requestedUserIds.isEmpty()) {
            if (!policy.allows(WorkflowTaskCapability.COPY)) {
                throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "当前节点未开放抄送能力");
            }
            copyUserIds.addAll(requestedUserIds);
        }
        if (copyUserIds.isEmpty() || !policy.allows(WorkflowTaskCapability.COPY)) {
            return;
        }
        List<Long> enabledUsers = validateEnabledUsers(new ArrayList<>(copyUserIds));
        Long historyTaskId = queryMapper.selectLatestHistoryIdByTaskId(task.getTaskId());
        if (historyTaskId == null) {
            throw new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), "任务历史记录尚未生成");
        }
        for (Long userId : enabledUsers) {
            WorkflowTaskCopy copy = new WorkflowTaskCopy();
            copy.setInstanceId(task.getInstanceId());
            copy.setHistoryTaskId(historyTaskId);
            copy.setBusinessId(task.getBusinessId());
            copy.setRecipientUserId(userId);
            copy.setSourceNodeCode(task.getNodeCode());
            copy.setSourceNodeName(task.getNodeName());
            copy.setMessage(message);
            copy.setAttachmentRef(attachmentRef);
            taskCopyMapper.insert(copy);
        }
    }

    /**
     * 将客户端下一节点选人转换为引擎内部变量，变量只在任务锁内生成并由分派监听器消费。
     */
    private Map<String, Object> prepareTaskVariables(WorkflowTaskVo task,
                                                     Map<String, Object> variables,
                                                     Map<String, List<Long>> nextAssignees) {
        Map<String, Object> result = new HashMap<>(safeTaskVariables(variables));
        List<WorkflowNodeSnapshot> nextNodes = previewNextNodes(task, result);
        Map<String, List<Long>> selections = nextAssignees == null ? Map.of() : nextAssignees;
        Set<String> nextNodeCodes = nextNodes.stream()
                .map(WorkflowNodeSnapshot::nodeCode)
                .collect(java.util.stream.Collectors.toSet());
        if (selections.keySet().stream().anyMatch(nodeCode -> !nextNodeCodes.contains(nodeCode))) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "动态办理人包含非法下一节点");
        }
        for (WorkflowNodeSnapshot node : nextNodes) {
            List<Long> userIds = selections.get(node.nodeCode());
            if (node.assigneeSelectionRequired() && (userIds == null || userIds.isEmpty())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "下一节点[" + node.nodeName() + "]必须选择办理人");
            }
            if (userIds != null && !userIds.isEmpty()) {
                if (!node.assigneeSelectionRequired()) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                            "下一节点[" + node.nodeName() + "]不允许动态指定办理人");
                }
                List<Long> enabledUsers = validateEnabledUsers(userIds);
                result.put(WorkflowConstants.INTERNAL_NEXT_ASSIGNEE_PREFIX + node.nodeCode(),
                        enabledUsers.stream().map(String::valueOf).toList());
            }
        }
        return result;
    }

    private List<WorkflowNodeSnapshot> previewNextNodes(WorkflowTaskVo task,
                                                        Map<String, Object> variables) {
        Instance instance = WorkflowEngineExceptionTranslator.execute(
                () -> insService.getById(task.getInstanceId()));
        if (instance == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
        }
        Map<String, Object> mergedVariables = new HashMap<>();
        if (instance.getVariableMap() != null) {
            mergedVariables.putAll(instance.getVariableMap());
        }
        mergedVariables.putAll(safeTaskVariables(variables));
        List<Node> nodes = WorkflowEngineExceptionTranslator.execute(
                () -> nodeService.getNextNodeList(task.getDefinitionId(), task.getNodeCode(),
                        null, SkipType.PASS.getKey(), mergedVariables));
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream().map(node -> toNodeSnapshot(task.getDefinitionId(), node)).toList();
    }

    private WorkflowNodeSnapshot toNodeSnapshot(Long definitionId, Node node) {
        WorkflowNodePolicy policy = nodePolicyService.getPolicy(definitionId, node.getNodeCode());
        return new WorkflowNodeSnapshot(
                node.getNodeCode(),
                node.getNodeName(),
                node.getNodeType(),
                policy.assigneeSelectionRequired());
    }

    private void enrichTaskCapabilities(WorkflowTaskVo task) {
        Set<WorkflowTaskCapability> capabilities = new LinkedHashSet<>(
                nodePolicyService.getPolicy(task.getDefinitionId(), task.getNodeCode()).capabilities());
        if (!hasPreviousUserTask(task)) {
            capabilities.remove(WorkflowTaskCapability.REJECT_LAST);
        }
        if (!StringUtils.hasText(task.getAllowedRejectNodeCode())) {
            capabilities.remove(WorkflowTaskCapability.REJECT_TO);
        }
        boolean hasRuntimeUser = activeTaskPermissions(task.getTaskId()).stream()
                .anyMatch(this::isRawUserId);
        if (!hasRuntimeUser) {
            capabilities.remove(WorkflowTaskCapability.REDUCE_SIGN);
        }
        task.setAllowedOperations(capabilities.stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    private boolean hasPreviousUserTask(WorkflowTaskVo task) {
        return queryMapper.selectHistoryByInstanceId(task.getInstanceId()).stream()
                .anyMatch(history -> Integer.valueOf(WorkflowConstants.USER_TASK_NODE_TYPE)
                        .equals(history.getNodeType())
                        && !task.getNodeCode().equals(history.getNodeCode()));
    }

    private void requireCapability(WorkflowTaskVo task, WorkflowTaskCapability capability) {
        WorkflowNodePolicy policy = nodePolicyService.getPolicy(task.getDefinitionId(), task.getNodeCode());
        if (!policy.allows(capability)) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(),
                    "当前节点未开放" + capability.name() + "能力");
        }
    }

    private List<String> activeTaskPermissions(Long taskId) {
        List<String> permissions = userService.getPermission(taskId,
                UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey(), UserType.DEPUTE.getKey());
        return permissions == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(permissions));
    }

    private List<Long> validateEnabledUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "目标用户不能为空");
        }
        LinkedHashSet<Long> distinctIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (distinctIds.isEmpty() || distinctIds.size() > 20) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "目标用户数量必须在1到20之间");
        }
        List<SysUser> users = sysUserService.list(new LambdaQueryWrapperX<SysUser>()
                .in(SysUser::getId, distinctIds)
                .eq(SysUser::getStatus, CommonStatusEnum.ENABLED.getCode()));
        Set<Long> enabledIds = users.stream().map(SysUser::getId).collect(java.util.stream.Collectors.toSet());
        if (enabledIds.size() != distinctIds.size()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "目标用户不存在或已停用");
        }
        return distinctIds.stream().toList();
    }

    private void validateOperationCommand(WorkflowTaskOperationCommand command, boolean usersOptional) {
        if (command == null || command.getTaskId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID不能为空");
        }
        if (!usersOptional && (command.getUserIds() == null || command.getUserIds().isEmpty())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "目标用户不能为空");
        }
        if (command.getUserIds() != null && command.getUserIds().size() > 20) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "目标用户数量不能超过20");
        }
        validateMessageAndAttachment(command.getMessage(), command.getAttachmentRef());
    }

    private boolean isRawUserId(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        try {
            Long.parseLong(permission);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private WorkflowInstanceSnapshot executeTask(Long taskId, TaskOperation taskOperation) {
        String lockKey = WorkflowConstants.WORKFLOW_LOCK_PREFIX + "task:" + taskId;
        // 归属校验和引擎写操作必须位于同一个任务锁及事务中，防止重复办理。
        return distributedLock.execute(lockKey, () -> transactionTemplate.execute(status -> {
            WorkflowTaskVo task = requireTask(taskId);
            requireTaskAccess(taskId);
            return taskOperation.execute(task);
        }));
    }

    private WorkflowInstanceSnapshot buildOperationResult(WorkflowTaskVo sourceTask,
                                                           Instance instance,
                                                           WorkflowOperation operation,
                                                           Long operatedTaskId) {
        Definition definition = requireDefinition(instance.getDefinitionId());
        Long currentTaskId = queryMapper.selectCurrentTaskId(instance.getId());
        publishEvent(operation, definition, instance, operatedTaskId, currentUserId(),
                sourceTask.getNodeCode(), instance.getNodeCode());
        return toSnapshot(definition, instance, currentTaskId);
    }

    private void requireInstanceAccess(Long instanceId) {
        long count = queryMapper.countInstanceAccess(instanceId, String.valueOf(currentUserId()), currentPermissions());
        if (count == 0) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "无权查看该流程实例");
        }
    }

    private WorkflowTaskVo requireTask(Long taskId) {
        if (taskId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID不能为空");
        }
        WorkflowTaskVo task = queryMapper.selectTaskById(taskId);
        if (task == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程任务不存在或已办理");
        }
        return task;
    }

    private void requireTaskAccess(Long taskId) {
        if (queryMapper.countTaskAccess(taskId, currentPermissions()) == 0) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "当前用户无权办理该任务");
        }
    }

    private Definition requirePublishedDefinition(String flowCode) {
        Definition definition = WorkflowEngineExceptionTranslator.execute(
                () -> defService.getPublishByFlowCode(flowCode));
        if (definition == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程定义未发布或不存在");
        }
        if (!ActivityStatus.ACTIVITY.getKey().equals(definition.getActivityStatus())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程定义已挂起");
        }
        return definition;
    }

    private Definition requireDefinition(Long definitionId) {
        if (definitionId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程定义ID不能为空");
        }
        Definition definition = WorkflowEngineExceptionTranslator.execute(() -> defService.getById(definitionId));
        if (definition == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程定义不存在");
        }
        return definition;
    }

    private WorkflowInstanceVo requireInstance(Long instanceId) {
        if (instanceId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程实例ID不能为空");
        }
        WorkflowInstanceVo instance = queryMapper.selectInstanceById(instanceId);
        if (instance == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
        }
        return instance;
    }

    private WorkflowInstanceVo requireInstanceByBusinessId(String businessId) {
        if (!StringUtils.hasText(businessId) || businessId.length() > 40) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "业务ID不能为空且长度不能超过40");
        }
        WorkflowInstanceVo instance = queryMapper.selectInstanceByBusinessId(businessId);
        if (instance == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
        }
        return instance;
    }

    private void publishEvent(WorkflowOperation operation,
                              Definition definition,
                              Instance instance,
                              Long taskId,
                              Long operatorId,
                              String sourceNodeCode,
                              String targetNodeCode) {
        // 发布器负责延迟到事务提交后发送，监听失败不反向回滚引擎操作。
        eventPublisher.publish(new WorkflowChangedEvent(
                UUID.randomUUID(), Instant.now(), operation, definition.getFlowCode(),
                definition.getId(), instance.getId(), instance.getBusinessId(), taskId,
                operatorId, sourceNodeCode, targetNodeCode, instance.getFlowStatus()));
    }

    private WorkflowStartResult toStartResult(Definition definition, Instance instance, Long taskId) {
        WorkflowStartResult result = new WorkflowStartResult();
        result.setDefinitionId(definition.getId());
        result.setInstanceId(instance.getId());
        result.setTaskId(taskId);
        result.setBusinessId(instance.getBusinessId());
        result.setFlowCode(definition.getFlowCode());
        result.setFlowName(definition.getFlowName());
        result.setNodeCode(instance.getNodeCode());
        result.setNodeName(instance.getNodeName());
        result.setFlowStatus(instance.getFlowStatus());
        return result;
    }

    private WorkflowInstanceSnapshot toSnapshot(Definition definition, Instance instance, Long taskId) {
        WorkflowInstanceSnapshot snapshot = new WorkflowInstanceSnapshot();
        snapshot.setDefinitionId(definition.getId());
        snapshot.setInstanceId(instance.getId());
        snapshot.setTaskId(taskId);
        snapshot.setBusinessId(instance.getBusinessId());
        snapshot.setFlowCode(definition.getFlowCode());
        snapshot.setFlowName(definition.getFlowName());
        snapshot.setNodeCode(instance.getNodeCode());
        snapshot.setNodeName(instance.getNodeName());
        snapshot.setFlowStatus(instance.getFlowStatus());
        return snapshot;
    }

    private WorkflowInstanceSnapshot toSnapshot(WorkflowInstanceVo instance, Long taskId) {
        WorkflowInstanceSnapshot snapshot = new WorkflowInstanceSnapshot();
        snapshot.setDefinitionId(instance.getDefinitionId());
        snapshot.setInstanceId(instance.getInstanceId());
        snapshot.setTaskId(taskId);
        snapshot.setBusinessId(instance.getBusinessId());
        snapshot.setFlowCode(instance.getFlowCode());
        snapshot.setFlowName(instance.getFlowName());
        snapshot.setNodeCode(instance.getNodeCode());
        snapshot.setNodeName(instance.getNodeName());
        snapshot.setFlowStatus(instance.getFlowStatus());
        return snapshot;
    }

    private <T> PageResponse<T> toPageResponse(IPage<T> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal());
    }

    private Long currentUserId() {
        return LoginUserInfoModelContext.getRequiredLoginUserId();
    }

    private List<String> currentPermissions() {
        List<String> permissions = loginUserSupport.getCurrentPermissions();
        if (permissions.isEmpty()) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        return permissions;
    }

    private Map<String, Object> safeVariables(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safeVariables = new HashMap<>(variables);
        safeVariables.entrySet().removeIf(entry -> entry.getValue() == null);
        return safeVariables;
    }

    private Map<String, Object> safeTaskVariables(Map<String, Object> variables) {
        Map<String, Object> safeVariables = new HashMap<>(safeVariables(variables));
        // 办理人和发起人变量只能由平台及流程 Provider 生成，任务请求不得覆盖。
        WorkflowConstants.PROTECTED_TASK_VARIABLES.forEach(safeVariables::remove);
        safeVariables.keySet().removeIf(key ->
                key != null && key.startsWith(WorkflowConstants.INTERNAL_NEXT_ASSIGNEE_PREFIX));
        return safeVariables;
    }

    private boolean targetContains(String allowedTargets, String targetNodeCode) {
        if (!StringUtils.hasText(allowedTargets) || !StringUtils.hasText(targetNodeCode)) {
            return false;
        }
        return Arrays.stream(allowedTargets.split(","))
                .map(String::trim)
                .anyMatch(targetNodeCode::equals);
    }

    private void validateStartCommand(WorkflowStartCommand command) {
        if (command == null || !StringUtils.hasText(command.getFlowCode())
                || !StringUtils.hasText(command.getBusinessId())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程编码和业务ID不能为空");
        }
        if (command.getFlowCode().length() > 100) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程编码长度不能超过100");
        }
        if (command.getBusinessId().length() > 40) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务ID长度不能超过40");
        }
        validateMetadata(command.getMetadata());
    }

    private void validateTaskCommand(WorkflowTaskCommand command) {
        if (command == null || command.getTaskId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID不能为空");
        }
        validateMessageAndAttachment(command.getMessage(), command.getAttachmentRef());
        validateCopyUsers(command.getCopyUserIds());
    }

    private void validateSubmitCommand(WorkflowSubmitCommand command) {
        if (command == null || !StringUtils.hasText(command.getFlowCode())
                || !StringUtils.hasText(command.getBusinessId())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程编码和业务ID不能为空");
        }
        if (command.getFlowCode().length() > 100) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程编码长度不能超过100");
        }
        if (command.getBusinessId().length() > 40) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务ID长度不能超过40");
        }
        validateMessageAndAttachment(command.getMessage(), command.getAttachmentRef());
        validateCopyUsers(command.getCopyUserIds());
        validateMetadata(command.getMetadata());
    }

    private void validateRejectCommand(WorkflowRejectCommand command) {
        if (command == null || command.getTaskId() == null
                || !StringUtils.hasText(command.getTargetNodeCode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "任务ID和退回节点不能为空");
        }
        if (command.getTargetNodeCode().length() > 100) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "退回节点编码长度不能超过100");
        }
        validateMessageAndAttachment(command.getMessage(), command.getAttachmentRef());
        validateCopyUsers(command.getCopyUserIds());
    }

    private void validateMessageAndAttachment(String message, String attachmentRef) {
        if (message != null && message.length() > 500) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "办理意见长度不能超过500");
        }
        if (attachmentRef != null && attachmentRef.length() > 500) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "附件引用长度不能超过500");
        }
    }

    private void validateCopyUsers(List<Long> copyUserIds) {
        if (copyUserIds != null && copyUserIds.size() > 20) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "抄送人数量不能超过20");
        }
    }

    private void validateMetadata(WorkflowBusinessMetadata metadata) {
        if (metadata == null) {
            return;
        }
        if (metadata.getBusinessCode() != null && metadata.getBusinessCode().length() > 100) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务编码长度不能超过100");
        }
        if (metadata.getBusinessTitle() != null && metadata.getBusinessTitle().length() > 500) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "业务标题长度不能超过500");
        }
    }

    @FunctionalInterface
    private interface TaskOperation {
        WorkflowInstanceSnapshot execute(WorkflowTaskVo task);
    }
}
