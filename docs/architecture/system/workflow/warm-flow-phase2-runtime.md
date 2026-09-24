# Warm-Flow 二阶段通用运行时设计

## 1. 目标与边界

- 提供流程发起、我的发起、待办、已办、实例详情、流程轨迹、通过、退回和撤销的通用运行时闭环。
- `vita-workflow` 声明不暴露 Warm-Flow 类型的工作流端口协议，并使用 Warm-Flow 官方 Service 实现状态变更；`vita-common`、`vita-service` 不承载工作流代码。
- Warm-Flow 8 张引擎表不生成 Controller、Entity、Mapper 和写 Service，仅保留专用只读投影 Mapper。
- 生产验收扩展新增最小报销业务表和状态同步；仍不实现通知、转办、委派、会签、加减签和实例状态人工修改。

## 2. 模块职责

| 模块 | 职责 |
| --- | --- |
| `vita-common` | 通用基础设施和基础模型，不放置工作流契约或实现 |
| `vita-workflow` | `WorkflowPort`、命令、快照与事件协议、报销流程域、Warm-Flow 适配、通用 Controller、只读关联查询、访问校验、锁和事件发布 |
| `vita-service` | 认证、RBAC、用户、角色、部门等系统业务能力，供 `vita-workflow` 单向查询 |
| `vita-admin` | 管理端启动配置、报销普通接口和报销管理补偿接口 |
| `vita-generator` | 阻止 8 张 Warm-Flow 引擎表生成业务 CRUD |

`vita-workflow` 是仓库中唯一允许使用 JOIN 的模块，且 JOIN 只能存在于专用只读 Query Mapper。其他模块、初始化 SQL、升级 SQL 和代码生成器均不得使用 JOIN。流程定义、实例和任务的写操作必须调用 Warm-Flow 官方 Service。

## 3. 公共 Java 协议

`WorkflowPort` 提供 `start`、`startAndPass`、`pass`、`rejectLast`、`rejectTo`、`cancel`、`getByBusinessId` 和 `canAccess`。命令和返回对象不暴露 Warm-Flow DTO。

`businessId` 是长度不超过 40 的全局唯一、不可复用业务键；业务模块应使用带业务域前缀的稳定值，例如 `REIMBURSE:20260720:10001`。

启动变量按以下顺序合并，后写入值覆盖同名值：

1. 客户端变量
2. 平台变量 `initiatorId`、`initiatorDeptId`
3. 匹配 `flowCode` 的 `WorkflowStartContextProvider` 变量

流程相关办理人变量必须由 Provider 生成。`start` 创建 `TOBESUBMIT` 实例并返回首个 `taskId`；业务模块使用 `startAndPass` 在同一业务锁和事务内创建实例并办理首任务。

## 4. 普通用户接口

实际接口地址统一带应用上下文 `/admin/api`。

| 方法和路由 | 行为 |
| --- | --- |
| `GET /workflow/definitions/published` | 分页查询已发布且激活的流程定义 |
| `POST /workflow/instances/start` | 创建待提交实例并返回首个任务 |
| `GET /workflow/instances/mine` | 分页查询当前用户发起的实例 |
| `GET /workflow/instances/{instanceId}` | 查询有权访问的实例 |
| `GET /workflow/instances/{instanceId}/history` | 查询有权访问的流程轨迹 |
| `POST /workflow/instances/{instanceId}/cancel` | 发起人在尚无审批人办理时撤回并终止实例 |
| `GET /workflow/tasks/pending` | 按用户、角色和部门权限分页查询待办 |
| `GET /workflow/tasks/completed` | 按历史实际办理人分页查询已办 |
| `GET /workflow/tasks/{taskId}` | 查询当前用户可办理的任务 |
| `POST /workflow/tasks/{taskId}/pass` | 通过任务或提交申请 |
| `POST /workflow/tasks/{taskId}/reject-last` | 退回上一人工节点 |
| `POST /workflow/tasks/{taskId}/reject-to` | 退回定义允许的指定节点 |

实例和轨迹只允许发起人、当前候选办理人或历史实际办理人查看。任务详情和办理先执行 be-vita 归属检查，再由 Warm-Flow 执行引擎权限校验。

## 5. 管理接口与 RBAC

管理接口必须同时通过 `@SaCheckPermission` 和 `SUB_ADMIN` / `SUPER_ADMIN` 角色校验。

| 权限码 | 接口范围 |
| --- | --- |
| `system:workflow:import` | `POST /system/workflow/importJson` |
| `system:workflow:definition:view` | 定义分页和详情 |
| `system:workflow:definition:manage` | 发布、取消发布、复制和删除定义 |
| `system:workflow:instance:view` | 运行中、已完成实例分页、详情和轨迹 |
| `system:workflow:reimbursement:view` | 报销单分页和详情 |
| `system:workflow:reimbursement:resync` | 按业务 ID 重同步报销状态 |

定义删除前检查是否已有实例；实例监控仅提供只读接口，不允许直接修改引擎表、任务或变量。

## 6. 并发、事务与事件

- 发起按 `vita:workflow:start:{businessId}` 加 Redis 分布式锁。
- 办理按 `vita:workflow:task:{taskId}` 加锁，撤回按 `vita:workflow:instance:{instanceId}` 加锁。
- 锁不等待，竞争失败返回 `423`；存在外层事务时锁在事务完成后释放，否则在 `finally` 中立即释放。
- `businessId` 重复校验由分布式锁内的数据库查询完成；本阶段不修改 Warm-Flow 引擎表索引。
- 状态变更事件在事务提交后发布；事务回滚不发布。
- `WorkflowChangedEvent` 不包含原始流程变量。同步监听器异常只记录日志，不改变已经提交的流程结果。

当前事件是进程内事件，不提供消息队列、Outbox 或跨进程可靠投递。

## 7. 数据库升级

新环境直接执行 `sql/init/workflow.sql`。已有环境按顺序执行：

```text
sql/upgrade/warm_flow_upgrade.sql
sql/upgrade/vita_workflow_upgrade.sql
```

`warm_flow_upgrade.sql` 会删除并重建全部 Warm-Flow 引擎表，永久清空流程定义、实例、任务、历史、办理人和表单数据，仅限明确接受清空风险时人工执行；`vita_workflow_upgrade.sql` 是常规幂等增量脚本，通过 `CREATE TABLE IF NOT EXISTS` 补齐分类和扩展表，并按业务编码补齐默认分类、角色、权限及管理员授权，不删除已有数据。引擎表继续使用 `sql/init/workflow.sql` 的原始索引，不增加增强索引。

## 8. 启停与发布

- 所有自建工作流 Bean 和 Controller 受 `vita.workflow.enabled` 控制。
- 报销服务与监听器位于 `vita-workflow` 并受同一开关控制；scheduler/openapi 不依赖该模块，因此不会扫描或装配工作流 Bean。
- 七个具体工作流 Controller 位于 `vita-workflow` 并由组件扫描直接注册；不保留 API/默认实现替换层，也不提供额外 YAML 分组开关。
- 设计器静态资源和 `/warm-flow-ui/config` 公开；Warm-Flow 表单加载和办理接口要求登录；其余 `/warm-flow/**` 限管理员角色；`/system/workflow/**` 执行权限码与管理员角色双重校验。
- 发布前备份需要保留的数据，再按文档顺序执行两个重建脚本并重新导入流程定义。

## 9. 报销生产最小闭环

- `workflow_reimbursement` 保存标题、费用类型、发生日期、金额、事由、申请人、部门、实例 ID 和业务状态。
- 状态为 `DRAFT`、`APPROVING`、`RETURNED`、`APPROVED`、`CANCELLED`；只有 `DRAFT` 和 `RETURNED` 可编辑和提交。
- 首次提交调用 `startAndPass`，退回申请节点后只办理现有任务，不重复创建实例。
- 流程事件提交后以独立事务同步业务状态；监听失败不回滚流程，管理员通过业务 ID 重同步补偿。
- 撤回只允许在申请人首次提交后、尚无其他人工办理记录时执行，调用 `terminationByInsId` 终止实例并清空待办；撤回后不可修改或重提。

## 10. 集成测试环境

`vita-admin/src/test/resources/application-workflow-integration.yml` 提供隔离 MySQL 8 和 Redis 配置，使用独立数据库与 Redis DB 14。通过 `VITA_WORKFLOW_IT_MYSQL_*`、`VITA_WORKFLOW_IT_REDIS_*` 环境变量覆盖连接信息。该配置不依赖 Docker，也不进入默认单元测试；执行全链路验收前需先初始化测试库并导入发布报销流程定义。
