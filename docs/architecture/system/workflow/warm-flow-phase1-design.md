# Warm-Flow 一期接入设计

## 1. 目标与参考边界

- 在 `vita-workflow` 内接入 Warm-Flow 1.8.8 引擎、设计器 SPI 和本地流程定义服务。
- 仅由 `vita-admin` 暴露设计器 UI，保持 `vita-admin -> vita-workflow -> vita-service -> vita-common` 依赖方向。
- 参考 RuoYi-Vue-Plus 的模块隔离、登录态复用和条件启停方式，不复制其租户、通知、SSE、请假示例及完整任务管理业务。
- 一期只完成引擎、设计器、权限处理器、路由认证、配置和 DDL 收口，不实现业务流程发起、待办、已办和实例管理闭环。

## 2. 依赖放置

- 根 `pom.xml` 统一维护 `warm-flow.version=1.8.8` 和三个 Warm-Flow 依赖版本。
- `vita-workflow` 引入 `warm-flow-mybatis-plus-sb3-starter` 与 `warm-flow-plugin-ui-core`。
- `vita-admin` 引入 `vita-workflow` 与 `warm-flow-plugin-ui-sb-web`。
- `vita-openapi`、`vita-scheduler` 不依赖 `vita-workflow`，不会被动暴露设计器资源或接口。

## 3. 配置与启停

`vita-admin/src/main/resources/application.yml` 通过 `spring.config.import` 引入 `application-workflow.yml`。

```yaml
vita:
  workflow:
    enabled: ${VITA_WORKFLOW_ENABLED:true}

warm-flow:
  enabled: ${vita.workflow.enabled}
  ui: ${vita.workflow.enabled}
  token-name: ${vita.sa-token.token-name}
  data-source-type: mysql
```

- `vita.workflow.enabled` 是 be-vita 唯一工作流开关；`vita-admin` 通过 workflow 配置资源默认启用，其他启动模块不依赖 `vita-workflow`。
- 引擎和设计器 UI 都映射到该开关，避免只关闭引擎但仍暴露 UI 控制器。
- `@ConditionalOnWorkflowEnabled` 应用于 be-vita 自建 workflow Bean、Advice 和控制器。
- 关闭后应用仍可启动，不注册本地 workflow 服务、控制器和 Advice，也不装配 Warm-Flow 引擎及 UI。

## 4. 路由认证矩阵

认证白名单只在 `vita-admin/src/main/resources/application.yml` 维护，profile 和 workflow 配置不得重复声明列表，避免 Spring 列表覆盖。

| 路由 | 匿名访问 | 登录用户 | 附加限制 |
| --- | --- | --- | --- |
| `/warm-flow-ui/index.html` | 允许 | 允许 | 仅设计器入口静态文件 |
| `/warm-flow-ui/css/**` | 允许 | 允许 | 静态资源 |
| `/warm-flow-ui/js/**` | 允许 | 允许 | 静态资源 |
| `/warm-flow-ui/ico/**` | 允许 | 允许 | 静态资源 |
| `/warm-flow-ui/config` | 允许 | 允许 | 设计器读取 token 配置 |
| Warm-Flow 表单和办理业务接口 | 拒绝 | 允许 | 仅开放 `published-form`、`form-content/**`、`execute/load/**`、`execute/hisLoad/**`、`execute/handle` |
| 其余 `/warm-flow/**` | 拒绝 | 条件允许 | 仅 `SUB_ADMIN` / `SUPER_ADMIN` 可访问设计和管理接口 |
| `/system/workflow/**` | 拒绝 | 条件允许 | 必须同时满足接口权限码和 `SUB_ADMIN` / `SUPER_ADMIN` 角色 |

禁止将 `/warm-flow/**` 或 `/warm-flow-ui/**` 整体加入 `vita.auth.extra-exclude-paths`。

## 5. 运行时装配

- `WorkflowPermissionHandler` 是受工作流开关控制的 Spring `@Component`。
- `permissions()` 返回 `user:<userId>`、当前用户的 `role:<roleCode>` 与 `dept:<deptId>`。
- `getHandler()` 返回当前用户 ID，用于历史办理记录留痕。
- `WorkflowConfiguration` 在应用启动完成后校验以下组件：
  - Warm-Flow 引擎配置已初始化
  - 引擎绑定的是 be-vita `WorkflowPermissionHandler`
  - `HandlerSelectService`
  - `CategoryService`
  - `IWorkflowDefinitionService`
- Warm-Flow 1.8.8 Starter 已在 `BeanConfig.initFlow()` 内设置 `FrameInvoker` 的配置和 Bean 获取函数，be-vita 不重复绑定。
- `WorkflowDefinitionPermissionNormalizer` 继续在首次导入或首次保存时为人工节点补齐管理员角色办理权限。
- `/warm-flow/**` 的管理员路由拦截由 `vita-workflow` 的 MVC 配置注册，`vita-common` 只保留模块无关的统一登录校验。

## 6. 接口范围

一期只新增一个本地管理接口：

- `POST /admin/api/system/workflow/importJson`
- 权限码：`system:workflow:import`
- 角色：`SUB_ADMIN` 或 `SUPER_ADMIN`
- 行为：导入原始 Warm-Flow `DefJson`，不自动发布。

Warm-Flow 内置 `/warm-flow/**` 同时包含业务办理和管理能力。普通用户只允许进入表单加载与办理所需接口，其余内置接口由管理员角色拦截。

当前不存在 `/system/workflow/reimburseApproval/create`，文档和权限映射不得声明该接口。

## 7. 数据库

`sql/init/workflow.sql` 包含 Warm-Flow 1.8.8 所需的 8 张表：

- `flow_definition`
- `flow_node`
- `flow_skip`
- `flow_instance`
- `flow_task`
- `flow_his_task`
- `flow_user`
- `flow_form`

be-vita 自建 `workflow_category`、`workflow_reimbursement` 继续遵循仓库审计字段规范。已有环境依次执行
`sql/upgrade/warm_flow_upgrade.sql` 和 `sql/upgrade/vita_workflow_upgrade.sql`。前者删除并重建全部 8 张
Warm-Flow 引擎表；后者是常规幂等增量脚本，只在缺失时创建 `workflow_category` 和业务扩展表，并按业务编码补齐默认分类与 RBAC 数据，不删除已有分类或报销单。执行破坏性引擎重建前必须备份需要保留的流程数据。

## 8. 第二阶段衔接

二阶段通用运行时已经独立落地，详细设计见 `docs/architecture/system/workflow/warm-flow-phase2-runtime.md`。

- `vita-workflow` 提供不暴露 Warm-Flow DTO 的工作流端口和事件协议，并实现定义、实例、任务、访问控制、报销流程域、并发锁和提交后事件编排。
- `vita-common`、`vita-service` 不承载工作流契约、业务域或实现；`vita-workflow` 仅单向调用 `vita-service` 的系统用户、角色和部门能力。
- `/workflow/**` 提供普通登录用户运行时接口，service 层校验实例访问权和任务办理权。
- `/system/workflow/**` 提供定义管理和实例只读监控，执行权限码与管理员角色双重校验。
- 报销流程已接入最小业务表、提交后状态监听和管理员重同步补偿，不包含通知、委派、转办和会签能力。
- 三阶段将通用定义、实例和任务 Controller 聚合到 `vita-workflow`，详细扩展方式见
  `docs/architecture/system/workflow/warm-flow-phase3-controller.md`；报销 Controller 继续保留在 `vita-admin`。

## 9. 验收测试

- 解析实际 YAML，断言白名单不含 `/warm-flow/**` 和 `/warm-flow-ui/**`。
- 使用实际 `/admin/api/...` URL 验证匿名、普通登录用户、子管理员和超级管理员访问边界。
- 验证 `/system/workflow/importJson` 同时执行权限码和角色校验。
- 验证 `vita.workflow.enabled=false` 时条件 Bean 不注册。
- 验证 `vita-scheduler`、`vita-openapi` 的依赖与组件扫描范围中不存在报销工作流服务和事件监听器。
- 验证引擎表生成 CRUD 不再注册，代码生成器拒绝再次生成。
- 编译全仓并执行 workflow/admin 目标测试。
