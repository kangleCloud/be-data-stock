# 报销审批流程 V1

## 1. 流程概览

- 流程编码：`REIMBURSE_APPROVAL_V1`
- 流程分类：`REIMBURSE`（展示名由 `workflow_category` 映射为“报销审批”）
- 流程路径：员工发起 -> 部门主管审批 -> 财务审批 -> 综合管理部审批 -> 总经理审批 -> 完成
- 已接入 `workflow_reimbursement` 最小业务表，以该定义验收创建、提交、逐级审批、退回、状态同步和撤回
- 业务状态为 `DRAFT`、`APPROVING`、`RETURNED`、`APPROVED`、`CANCELLED`、`TERMINATED`、`INVALIDATED`

## 2. 节点编码

| 节点名称 | 节点编码 | 办理人配置 |
| --- | --- | --- |
| 员工发起 | `submit_apply` | `${applicantPermission}`、`role:SUPER_ADMIN`、`role:SUB_ADMIN` |
| 部门主管审批 | `dept_leader_approve` | `${deptLeaderPermission}`、`role:SUPER_ADMIN`、`role:SUB_ADMIN` |
| 财务审批 | `finance_approve` | `role:FINANCE_APPROVER`、`role:SUPER_ADMIN`、`role:SUB_ADMIN` |
| 综合管理部审批 | `general_affairs_approve` | `role:GENERAL_AFFAIRS_APPROVER`、`role:SUPER_ADMIN`、`role:SUB_ADMIN` |
| 总经理审批 | `general_manager_approve` | `role:GENERAL_MANAGER_APPROVER`、`role:SUPER_ADMIN`、`role:SUB_ADMIN` |
| 完成 | `end` | 无 |

说明：

- `submit_apply` 保留为显式用户任务，不省略。
- 首次导入和首次保存新流程时，所有人工节点默认补齐 `role:SUPER_ADMIN` 与 `role:SUB_ADMIN`。
- 超级管理员通过虚拟角色码 `SUPER_ADMIN` 并入人工节点办理人集合。

## 3. 办理人协议

- 用户：`user:<userId>`
- 角色：`role:<roleCode>`
- 部门：`dept:<deptId>`

运行期变量要求：

- `applicantPermission = user:<发起人ID>`
- `deptLeaderPermission = user:<发起人部门负责人ID>`

`deptLeaderPermission` 固定按以下链路解析：

- `sys_user.dept_id -> sys_dept.leader_user_id`

失败约束：

- 发起人无部门，直接失败
- 部门无 `leader_user_id`，直接失败
- 审批角色下无人时，流转时报错

## 4. 退回规则

- 员工提交/重提：`TaskService.pass`
- 返回上一级：`TaskService.rejectLast`
- 退回申请人：`TaskService.rejectAtWill(taskId, "submit_apply", ...)`

节点行为说明：

- `dept_leader_approve` 节点，“返回上一级”和“退回申请人”都会回到 `submit_apply`
- `finance_approve`、`general_affairs_approve`、`general_manager_approve` 同时保留两种退回路径
- 当前报销定义不做金额分支；通用运行时已支持按节点显式配置加签、抄送和终止

## 5. 设计器选人协议

- 设计器开放“用户 / 角色 / 部门 / 规则”页签
- 用户候选来自 `sys_user`
- 角色候选来自 `sys_role`
- 部门候选来自 `sys_dept`
- `SUPER_ADMIN` 在设计器里只展示一条虚拟角色项，不回显物理 `SUPER_ADMIN` 角色记录
- 员工发起和部门主管节点统一通过变量表达式配置，不通过选择器选人
- 新流程可选择内置 `DEPT_LEADER` 受控规则，平台生成固定桥接表达式，不接受任意 SpEL

## 6. 角色准备

升级脚本：`sql/upgrade/vita_workflow_upgrade.sql`

该脚本以增量方式补齐 `workflow_category`、默认分类、报销、业务元数据、抄送和办理人规则表，不清空已有分类或业务数据；审批角色、工作流权限和管理员授权按业务编码幂等补齐。

初始化三类物理角色：

- `FINANCE_APPROVER`
- `GENERAL_AFFAIRS_APPROVER`
- `GENERAL_MANAGER_APPROVER`

特殊说明：

- 不新增物理 `SUPER_ADMIN` 角色记录
- `SUPER_ADMIN` 继续表示 be-vita 的超级管理员虚拟角色语义
- `SUB_ADMIN` 继续使用 `sys_role` 中的物理角色记录

## 7. 产物清单

- 流程定义 JSON：`docs/requirements/workflow/definitions/reimburse-approval-v1.json`
- 当前说明文档：`docs/requirements/workflow/reimburse-approval-v1.md`

导入说明：

- 流程定义使用 Warm-Flow `DefJson` 结构留档
- 可通过 `POST /admin/api/system/workflow/importJson` 直接导入当前 JSON
- 调用时携带 `Authorization: Bearer <token>`，并要求 `system:workflow:import` 权限和 `SUB_ADMIN` 或 `SUPER_ADMIN` 角色
- 导入不会自动发布；管理员通过 `POST /admin/api/system/workflow/definitions/{definitionId}/publish` 发布
- 新环境创建前执行 `sql/init/workflow.sql`；已有环境重建时依次执行
  `sql/upgrade/warm_flow_upgrade.sql` 和 `sql/upgrade/vita_workflow_upgrade.sql`
- `warm_flow_upgrade.sql` 会永久清空全部流程定义、实例、任务、历史和表单数据
- 若设计器导入后发生坐标微调，以设计器重新保存结果为准，不影响节点编码、办理人协议与退回规则

## 8. 报销业务接口

先创建草稿：

```http
POST /admin/api/workflow/reimbursements
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "七月客户拜访差旅报销",
  "expenseType": "TRAVEL",
  "expenseDate": "2026-07-20",
  "totalAmount": 1280.50,
  "reason": "客户现场沟通差旅费用"
}
```

草稿创建后由服务端生成 `businessId=REIMBURSE:<报销单ID>`。`DRAFT` 和 `RETURNED` 可通过 `POST /admin/api/workflow/reimbursements/{id}` 修改，其余状态只读。

首次提交在同一事务中创建实例并办理员工首任务：

```http
POST /admin/api/workflow/reimbursements/{id}/submit
Authorization: Bearer <token>
Content-Type: application/json

{
  "message": "提交报销申请",
  "variables": {}
}
```

服务端根据当前登录用户生成 `applicantPermission`，并按 `sys_user.dept_id -> sys_dept.leader_user_id` 生成 `deptLeaderPermission`；客户端同名值会被覆盖。部门或部门负责人缺失时，提交直接返回业务错误。

后续审批人从 `GET /admin/api/workflow/tasks/pending` 获取本人待办并调用通用 `pass` 接口。退回申请人后报销单进入 `RETURNED`，申请人修改后再次调用报销 `submit`，只办理现有申请节点任务。

申请人可在尚无审批人办理时调用 `POST /admin/api/workflow/reimbursements/{id}/withdraw`。撤回使用 `TaskService.terminationByInsId` 终止实例并清空待办，报销单进入 `CANCELLED` 终态，不允许修改或重提；重新申请必须新建报销单。

节点授权的候选办理人调用通用终止接口后，报销单进入 `TERMINATED`；管理员作废实例后进入
`INVALIDATED`。实例挂起和恢复不改变报销业务状态。三个状态均不允许申请人修改或重提。

查询接口：

- `GET /admin/api/workflow/reimbursements/mine`：我的报销分页
- `GET /admin/api/workflow/reimbursements/{id}`：申请人或流程参与人查看详情
- `GET /admin/api/system/workflow/reimbursements/page`：管理员全局分页
- `GET /admin/api/system/workflow/reimbursements/{id}`：管理员详情
- `POST /admin/api/system/workflow/reimbursements/resync?businessId=...`：管理员状态补偿
