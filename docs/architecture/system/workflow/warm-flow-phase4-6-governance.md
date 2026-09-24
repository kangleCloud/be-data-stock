# Warm-Flow 四至六阶段高级任务与治理设计

## 1. 实施范围

本阶段在既有通用运行时和 Controller 聚合基础上补充高级任务、节点扩展和管理治理。引擎版本保持
Warm-Flow `1.8.8`，所有引擎状态写操作继续调用官方 `TaskService`、`InsService`、`DefService`
和 `NodeService`，不新增八张引擎表的实体或写 Mapper。

已实现能力：

- 下一节点预览、可退回节点、转办、委派、加签、减签和候选人终止。
- 节点任务能力、默认抄送用户和下一节点动态选人配置。
- 抄送列表和抄送参与人的实例只读权限。
- 定义 JSON 查看/下载、定义激活/挂起。
- 实例激活/挂起、管理员作废、全局待办和已办。
- 分类树、分类 CRUD、循环和引用校验。
- 受控办理人规则、固定表达式桥接和部门负责人解析器。
- 通用业务标题元数据、附件不透明引用和提交后状态事件。
- 业务 ID 查询、安全流程图、变量白名单、当前办理人和受限实例清理。

本阶段仍不实现站内信、短信、邮件、催办、岗位候选人、完整租户隔离、Outbox/MQ 和任意变量修改。
通知和租户空端口已删除。`WorkflowHandlerSource` 作为实际使用的办理人来源扩展点按可选集合注入，
没有扩展实现时不注册占位来源。

## 2. 节点能力

设计器节点扩展固定使用以下编码：

| 编码 | 用途 |
| --- | --- |
| `VitaTaskCapability` | 配置服务端允许的任务操作 |
| `VitaCopyUser` | 配置默认抄送用户 |
| `VitaNextAssignee` | 配置使用定义候选人或办理时动态选人 |

旧流程未配置 `VitaTaskCapability` 时只开放 `PASS`、`REJECT_LAST` 和 `REJECT_TO`，高级操作全部拒绝。
任务详情中的 `allowedOperations` 由服务端根据节点配置、历史轨迹和当前候选人重新计算。接口办理时
仍会在任务锁内再次校验，前端按钮状态不构成授权依据。

动态选人请求按下一节点编码提交用户 ID。服务端先预览下一节点，再校验节点存在、节点允许动态选人
且用户处于启用状态，之后生成 `_vitaNextAssignee:<nodeCode>` 内部变量。分派监听器消费并移除该变量，
客户端不能直接写入此命名空间。

## 3. 高级任务接口

| 方法和路由 | 行为 |
| --- | --- |
| `POST /workflow/tasks/{id}/next-nodes` | 预览下一节点和动态选人要求 |
| `GET /workflow/tasks/{id}/rejectable-nodes` | 查询允许退回节点 |
| `POST /workflow/tasks/{id}/transfer` | 转办给一个启用用户 |
| `POST /workflow/tasks/{id}/delegate` | 委派给一个启用用户 |
| `POST /workflow/tasks/{id}/add-sign` | 增加一个或多个运行期办理人 |
| `POST /workflow/tasks/{id}/reduce-sign` | 移除运行期增加的原始用户候选人 |
| `POST /workflow/tasks/{id}/terminate` | 当前候选人按节点授权终止流程 |
| `GET /workflow/tasks/copied` | 查询当前用户收到的抄送 |

转办、委派、加签和减签统一调用 `TaskService.updateHandler`。转办和委派只接受一个目标用户；加签不移除
定义候选人；减签不能移除 `user:*`、`role:*`、`dept:*` 定义候选人，且操作后至少保留一个办理权限。
所有高级操作继续执行任务归属检查、Warm-Flow 权限校验、`taskId` 分布式锁和事务。

## 4. 分类和办理人规则

分类编码创建后不可修改。修改父分类时沿父链检查循环；删除前检查子分类和 `flow_definition.category`
引用。分类管理接口使用 `system:workflow:category:view` 和
`system:workflow:category:manage`。

办理人规则只保存规则编码、解析器编码和参数名，不保存管理员输入的原始 SpEL。平台按固定模板生成：

```text
#{@workflowAssigneeRuleBridge.resolve('<规则编码>',#<参数名>...)}
```

默认规则 `DEPT_LEADER` 使用解析器 `DEPARTMENT_LEADER` 和参数 `deptId`，同时校验部门、负责人和用户状态。
规则删除前检查 `flow_node.permission_flag` 引用。规则管理接口使用
`system:workflow:assignee-rule:view` 和 `system:workflow:assignee-rule:manage`。

办理人选择器按来源编码聚合用户、角色、部门和规则四个内置来源。业务模块可以实现
`WorkflowHandlerSource` 增加岗位等可信来源；扩展编码不得覆盖内置编码，没有实现时不暴露占位来源。

## 5. 管理治理

| 权限码 | 接口范围 |
| --- | --- |
| `system:workflow:definition:manage` | 定义导出、激活和挂起 |
| `system:workflow:instance:manage` | 实例激活、挂起和作废 |
| `system:workflow:task:view` | 全局待办和已办 |
| `system:workflow:task:manage` | 修改运行中任务办理人 |
| `system:workflow:category:view` | 分类查询 |
| `system:workflow:category:manage` | 分类写操作 |
| `system:workflow:assignee-rule:view` | 受控办理人规则查询 |
| `system:workflow:assignee-rule:manage` | 受控办理人规则写操作 |

所有 `/system/workflow/**` 具体 Controller 方法直接声明 `@SaCheckPermission`，并调用
`WorkflowAdminAccessChecker` 要求 `SUPER_ADMIN` 或 `SUB_ADMIN`。管理员作废使用
`TaskService.terminationByInsId` 和 `NULLIFY` 状态。物理删除仅允许无业务元数据、无报销引用的实例，
运行实例和历史实例分别处理；操作在实例锁和事务内通过 Warm-Flow 官方 Service 清理引擎数据，再清理
自建抄送记录。

实例变量查询会过滤平台身份、候选人和 `_vita` 内部变量。变量修改由
`WorkflowVariablePolicy` 按 `flowCode` 声明可写键，未注册流程保持只读；修改事件不携带变量值。
流程图只返回节点、连线、当前节点和历史状态投影，不返回候选表达式、跳转条件、原始变量或定义 JSON。

`GET /workflow/tasks/{taskId}/handlers` 返回当前任务候选人。管理员通过
`POST /system/workflow/tasks/{taskId}/handlers` 将候选人替换为启用用户，至少保留一名办理人，并在
任务锁内复核用户状态后调用 `TaskService.updateHandler`。

## 6. 数据边界

新增自建表：

- `workflow_instance_metadata`：业务 ID、实例 ID、流程编码、业务编码和标题快照。
- `workflow_task_copy`：来源历史任务、抄送用户、节点、意见和附件引用。
- `workflow_assignee_rule`：规则编码、解析器编码、参数声明和状态。

初始化脚本为 `sql/init/workflow.sql`，已有环境使用集中脚本
`sql/upgrade/vita_workflow_upgrade.sql`。Vita 升级脚本是幂等增量脚本，`workflow_category` 和三张
扩展表均只做 `CREATE TABLE IF NOT EXISTS`，默认分类、权限和角色授权按业务编码补齐，不删除已有数据。
`warm_flow_upgrade.sql` 和八张 Warm-Flow 引擎表结构、索引保持原始版本，不增加业务唯一索引或分页索引。
升级 SQL 不使用 JOIN；JOIN 只允许存在于 `vita-workflow` 专用只读
`WorkflowRuntimeQueryMapper.xml`。

## 7. 报销状态

报销首次提交写入 `businessCode=REIMBURSEMENT` 和标题快照。候选人终止映射为 `TERMINATED`，管理员作废映射
为 `INVALIDATED`，两者均为只读终态；实例挂起和恢复不修改报销业务状态。既有未审批撤回继续映射为
`CANCELLED`。
