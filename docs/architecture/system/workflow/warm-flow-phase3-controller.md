# Warm-Flow Controller 收口设计

## 1. 目标

- 所有工作流 HTTP 边界集中在 `vita-workflow`。
- 保留现有 `/workflow/**`、`/system/workflow/**` 路由和请求响应结构。
- Controller 仅负责参数校验、鉴权、日志、重复提交、DTO 转换和 Service 调用。
- 不提供 Controller API/默认实现双层结构，也不提供按能力域替换 Bean 的机制。
- 仅保留 `vita.workflow.enabled` 总开关；`vita-admin` 导入 workflow 配置后缺省为 `true`，其他启动模块未配置时不启用报销工作流 Bean。

## 2. Controller 结构

| Controller | 能力 |
| --- | --- |
| `WorkflowDefinitionController` | 已发布定义、定义管理、JSON 导入和导出 |
| `WorkflowInstanceController` | 发起、实例查询、轨迹、流程图、撤回和实例治理 |
| `WorkflowTaskController` | 待办、已办、通过、退回、高级任务和办理人治理 |
| `WorkflowCategoryController` | 分类树、详情和安全 CRUD |
| `WorkflowAssigneeRuleController` | 受控办理人规则管理 |
| `WorkflowReimbursementController` | 普通用户报销闭环 |
| `WorkflowReimbursementAdminController` | 报销管理查询和状态补偿 |

上述类均位于 `com.vita.workflow.controller`。`vita-admin` 不再保留工作流 Controller。
`controller.api`、`controller.impl` 和 `WorkflowControllerConfiguration` 已删除，具体 Controller
由应用的 `com.vita` 组件扫描直接注册。

Controller 禁止依赖 Mapper、Warm-Flow Service、事务模板和事件发布器。流程编排集中在
`WorkflowRuntimeService`，具体报销业务集中在 `WorkflowReimbursementService`。

## 3. 权限边界

- 定义分页、详情和 JSON 查看：`system:workflow:definition:view`
- 发布、取消发布、复制、删除、导出和激活：`system:workflow:definition:manage`
- JSON 导入：`system:workflow:import`
- 实例分页、详情、轨迹、流程图和变量查看：`system:workflow:instance:view`
- 实例变量修改、激活、挂起、作废和受限删除：`system:workflow:instance:manage`
- 全局待办、已办：`system:workflow:task:view`
- 任务办理人修改：`system:workflow:task:manage`
- 分类查询和管理：`system:workflow:category:view`、`system:workflow:category:manage`
- 办理人规则查询和管理：`system:workflow:assignee-rule:view`、`system:workflow:assignee-rule:manage`
- 报销查询和状态重同步：`system:workflow:reimbursement:view`、
  `system:workflow:reimbursement:resync`

所有 `/system/workflow/**` 方法直接声明 `@SaCheckPermission`，并调用
`WorkflowAdminAccessChecker.check()`，同时要求 `SUPER_ADMIN` 或 `SUB_ADMIN`。普通
`/workflow/**` 接口继续依赖登录态及流程参与人校验。

## 4. AOP 与总开关

写接口按行为声明 `@Log` 和 `@RepeatSubmit`。`RepeatSubmitAspect` 使用
`com.vita..controller..*Controller` 切点，因此 `vita-workflow` 中的 Controller 仍受保护。

所有 Controller 使用 `@ConditionalOnWorkflowEnabled`。设置
`vita.workflow.enabled=false` 时七个 Controller 与工作流运行时 Bean 一并停止注册；不增加其他
YAML Controller 开关。

## 5. 验收

- 七个具体 Controller 默认注册，全部路由唯一。
- `vita-admin` 不存在工作流 Controller。
- `/system/workflow/**` 方法直接声明正确权限码并执行管理员角色校验。
- Controller 不依赖 Mapper、Warm-Flow Service、事务或事件发布器。
- 总开关关闭后全部工作流 Controller 不注册。
- 迁移前已有 URL、HTTP 方法和响应结构保持不变。
