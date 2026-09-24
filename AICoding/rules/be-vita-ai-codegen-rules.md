# be-vita AI 编码强制规则

## 规则定位

本文档是 `be-vita` 仓库中 AI 生成、修改、审查和重构代码时唯一完整、最高优先级的强制规则源。

- 仓库结构、模块说明和命令入口以根目录 `README.md` 为准。
- 任务规划、Skill 路由和交付流程以根目录 `AGENTS.md` 为准。
- `AICoding/skills/be-vita-*` 与 `AICoding/skills/common-*` 只编排任务流程，不得重新定义或放宽本文件约束；发生冲突时以本文件为准。

## 模块与依赖边界

- 必须保持 `vita-admin -> vita-service -> vita-common`、`vita-admin -> vita-workflow -> vita-service -> vita-common`、`vita-app -> vita-service -> vita-common`、`vita-openapi -> vita-service -> vita-common`、`vita-scheduler -> vita-service -> vita-common` 的依赖方向。
- 必须统一使用 `com.vita.*` 作为包前缀。
- 跨模块协作必须通过公开服务接口完成，不得直接访问其他模块的内部 Mapper、实现类或配置细节。
- 通用能力必须沉淀到 `vita-common` 或合适的 starter，不得为临时复用破坏依赖边界。
- 工作流契约、领域逻辑和实现必须保留在 `vita-workflow`，不得下沉到 `vita-common` 或 `vita-service`。

## 分层职责

- Controller 只负责接收参数、鉴权声明、调用 Service 和返回统一响应，不得直接调用 Mapper。
- 工作流定义、实例、任务及具体流程业务 Controller 必须聚合在 `vita-workflow`；非工作流 Controller 保留在对应启动模块，不得放入所有应用共同依赖的 `vita-service`。
- Service 负责业务编排、参数校验、事务控制、跨表聚合和状态流转。
- Mapper 默认只负责单表持久化、条件封装、字段投影、计数、排序和必要的 XML。
- Entity 只表达数据库表结构，不承载 Controller 语义或 Web 校验规则。

## 统一技术基线

- Java 实现必须兼容 JDK 17 及仓库现有 Maven、Spring Boot 依赖基线，未经任务明确要求不得升级基础技术栈。
- Web 返回体必须使用 `CommonResult<T>`。
- 分页请求必须使用 `PageRequest`，分页响应必须使用 `PageResponse<T>`。
- MyBatis-Plus 查询必须优先使用 `BaseMapperX` 与 `LambdaQueryWrapperX`。
- 参数校验必须使用 `jakarta.validation`。
- `/system/*` 管理端接口默认使用 `@SaCheckPermission("system:资源:动作")`。
- 写操作事务默认在外部入口方法上使用 `@Transactional(rollbackFor = Exception.class)`。
- 不得新增第二套返回体、分页对象、异常语义或同类基础框架。

## 命名与目录

- 源码文件必须使用 UTF-8；Java 代码使用 4 空格缩进，并遵循目标文件已有的导入、注解和空行风格。
- 未经任务明确要求，不得批量格式化无关代码或引入新的格式化工具。
- 类名使用 `PascalCase`，方法名和字段名使用 `camelCase`，常量使用 `UPPER_SNAKE_CASE`，包名必须全小写。
- 类型后缀必须保持一致：`*Controller`、`*Service`、`*ServiceImpl`、`*Mapper`、`*CreateDto`、`*UpdateDto`、`*SearchDto`、`*DetailVo`、`*ListVo`、`*PageVo`、`*OptionVo`、`*Property`。
- 配置类必须放在领域级 `property` 包中，使用单数 `XxxProperty` 命名；不得新增 `*Properties`，也不得将配置字段散落到 `config`、`service` 或 `support` 中。

## 持久化与 SQL

- 单表查询必须优先使用 `BaseMapperX + LambdaQueryWrapperX`，条件优先使用现有 `eqIfPresent`、`likeIfPresent`、`inIfPresent` 等扩展。
- 单表字段投影、计数、排序和去重必须下沉到 Mapper，不得在 Service 中以 `selectList().stream().map()` 替代。
- 普通关联查询必须遵循“先查关联 ID -> 去重 -> 再查目标表 -> Service 聚合组装”。
- 仅 `vita-workflow` 可在专用只读 Query Mapper 中使用 `join` 完成流程定义、实例、任务、办理人和轨迹投影。
- 工作流引擎表状态写入必须调用 Warm-Flow 官方 Service，不得通过关联 SQL 或自建写 Mapper 修改。
- 代码生成器不得生成 `join` SQL；工作流只读关联查询必须手工设计。
- 只有 XML 或手写 SQL 才显式补充 `is_deleted = 0`。

## 事务、校验与异常

- 同一 Service 中多个 `public @Transactional` 方法不得互相直接调用。
- 共用事务逻辑必须抽取为 `private` 方法，不得使用 `self` 注入或 `AopContext.currentProxy()` 绕过代理边界。
- Service 必须校验空值、关键 ID、登录上下文、数据存在性和操作合法性。
- 参数非法必须抛出 `ServiceException(GlobalErrorCode.BAD_REQUEST)`。
- 数据不存在必须抛出 `ServiceException(GlobalErrorCode.NOT_FOUND)`。

## 文档联动

以下变更必须同步更新对应文档或 SQL：

- 认证链路、RBAC 权限规则和日志方案
- Starter 配置项和自动装配行为
- 数据库表结构、初始化脚本和升级脚本
- 生成器模板输出规范

需求与验收放在 `docs/requirements/`，技术详设放在 `docs/architecture/`，开发与运维指南放在 `docs/development/`；数据库脚本放在 `sql/init/` 或 `sql/upgrade/`。仓库文档不得写入个人机器绝对路径。文档和实现冲突时必须明确指出，并以当前源码、数据库脚本和可执行验证结果复核。

## 标准 CRUD 与 vita-generator

- 新增或变更表后如需 Entity、DTO、VO、Mapper、Service、Controller、Model 等标准 CRUD 骨架，必须先以 `vita-generator` 模板输出为起点。
- 允许在模板输出上进行必要的业务补充、裁剪和重构，但不得绕过模板从零手写风格不一致的 CRUD，也不得将手写近似骨架描述为生成结果。
- 该约束适用于普通业务表、日志表和 workflow 业务扩展表等仓库自建表；纯关联表、第三方组件内置表及明确无需标准 CRUD 的场景不触发。
- 模板输出必须继续遵守本文件的统一技术基线、分层、持久化、事务、异常和 DDL 约束。
- 修改模板时必须检查生成结果是否引入新目录约定、命名后缀、基础类型或不符合规则的持久化代码。

## 核心业务中文注释

- 核心业务代码必须在关键业务规则、事务边界、状态流转、并发控制、外部系统集成假设、特殊校验和重要异常处理处补充中文注释。
- 注释必须解释原因、约束和边界，不得重复代码表面含义。
- getter/setter、直白的 CRUD 转发、模板骨架和可从命名直接理解的简单语句不要求逐行注释。

## CodeGraph 边界

- CodeGraph 仅是可选的开发期本地分析工具，不属于 Maven 依赖、应用运行时、部署组件或 CI 门禁。
- 团队版本固定为 `1.5.0`；仓库级操作必须使用 `./tools/codegraph/codegraph.sh`，相关脚本只能位于 `tools/codegraph/`。个人 MCP 内部执行官方 `codegraph serve --mcp` 是唯一例外。
- 不得删除或绕过仓库级入口，不得新增模块级别名、自定义启动入口，也不得提交 `.codegraph/`、本地索引或个人 MCP 配置。
- CodeGraph 结果只作辅助依据；多级接口、反射、框架绑定等动态调用必须用 `rg` 和源码复核。
- 工具不可用、索引过期或结果不明确时必须回退到精确搜索和源码读取，不得阻塞任务；源码、SQL、编译和测试结果拥有最终优先级。
- 安装、维护、输出限制、故障处理和回滚见 `docs/development/codegraph-integration.md`。

## 应该遵守

- 应该为新增业务逻辑、异常分支、参数校验和关键状态流转补充测试。
- 设计先行的任务应该先更新文档，再实现代码。
- 应该优先复用仓库现有骨架和模式，避免增加平行实现。
- 修改生成器模板时应该对代表性输出执行编译或专项验证。

## 禁止事项

- 禁止在 Controller 中编写复杂业务判断、跨表组装或第三方 SDK 调用。
- 禁止直接输出密码、Token、数据库密码、模型 API Key 等敏感信息。
- 禁止把 Java、Mapper、SQL、配置资源或其他业务代码放入 `tools/`。
- 禁止把需求或详细设计散落到 `vita-*` Maven 子模块中。
- 禁止新增绕过仓库级既有根命令的模块级别名或自定义启动脚本。

## 新建表默认字段 DDL 规范

### 适用范围

本规范适用于除纯关联表外的所有新表，包括：

- 业务主表
- 日志表
- 需要审计、逻辑删除、租户隔离或乐观锁控制的扩展表

例外规则：

- `sys_user_role`、`sys_role_menu` 等纯中间关系表可按场景精简字段。
- 纯关联表不强制带完整审计后缀，但必须在设计文档或 SQL 中说明原因。
- Warm-Flow 官方引擎表可按官方 DDL 原样落库，不强制补齐本仓库公共字段或改写字段顺序。
- be-vita 自建的 workflow 扩展表、流程业务表和审批关联表仍必须完整遵守本规范。

本规范不要求批量追溯改写全部历史表；历史表以渐进方式对齐。

### 强制字段

除纯关联表外，新建表默认公共字段必须包含：

- `tenant_id`
- `create_time`
- `update_time`
- `is_deleted`
- `create_by_id`
- `create_by`
- `update_by_id`
- `update_by`
- `version`
- `remark`

### 字段顺序

新建表字段布局必须保持：

```text
id -> tenant_id -> 业务字段 -> 公共审计后缀
```

- `tenant_id` 固定放在 `id` 后。
- 业务字段放在中间，审计后缀固定收口在尾部。
- 业务专属字段可放在业务字段区域，例如 `sys_user.pwd_update_date`，不得混入公共审计后缀内部。

### 默认值约束

- `create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
- `is_deleted TINYINT(1) DEFAULT 0`
- `version BIGINT DEFAULT 0`
- `tenant_id` 默认 `NULL`
- `create_by_id`、`create_by`、`update_by_id`、`update_by` 默认 `NULL`
- `remark` 默认 `NULL`

### 代表性样例

`sys_user` 作为“业务主表 + 公共审计后缀”的代表性样例：

```sql
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              bigint                                                        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`       bigint                                                                 DEFAULT NULL COMMENT '租户ID',
    ...业务字段...,
    `pwd_update_date` datetime                                                               DEFAULT NULL COMMENT '密码最后更新时间',
    `create_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`     datetime                                                               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1)                                                              DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`    bigint                                                                 DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`    bigint                                                                 DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                                                                 DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息表';
```

## 模块专项规则

- 仓库自建资源的单条删除接口统一使用 `POST /delete` 和 `@RequestBody @Valid *DeletedDto`，请求体为 `{"id": 主键值}`；Controller 与 Service 均传递完整 DTO。
- 批量删除接口统一使用 `POST /deleteBatch` 和 `@RequestBody @Valid *BatchDeletedDto`，请求体为 `{"ids": [主键值]}`，ID 列表及元素都不得为空。
- Warm-Flow 自带的流程定义、运行实例、按业务 ID 及历史实例删除接口遵循引擎既有路由和参数契约，不套用上述自建资源删除规范。

### `vita-admin` / `vita-app` / `vita-service` / `vita-common`

- 管理端 CRUD 命名沿用 `add`、`update`、`delete`、`getDetail`、`list`、`page`。
- 普通业务模块默认提供 `create`、`update`、`delete`、`get`、`list`、`page` 六类 Service 方法。
- 扩展方法优先保持现有风格，例如 `getAll`、`getOptionListByUserId`、`check*Unique`、`auth*`、`cancelAuth*`。
- `vita-admin`、`vita-app` 等启动模块只保留启动类、Controller和运行配置，非工作流业务逻辑统一下沉到 `vita-service`。

### `vita-openapi` / `vita-scheduler`

- 只读公开接口必须保持薄 Controller 和薄启动模块，不得在启动模块中承载复杂业务编排。
- 调度触发接口只负责触发、鉴权和返回结果，核心刷新逻辑必须下沉到 `vita-service`。

### `vita-starters`

- 自动装配类只负责装配和条件控制，不承担业务逻辑。
- 配置项必须放入独立 `property` 类。
- starter 不得反向依赖具体业务模块。

## pre-v1.0 重构原则

当前项目处于 pre-v1.0 阶段，暂无需要保护的稳定外部消费者或 API 契约。

- 除非任务明确要求，不得为历史实现新增兼容分支、兼容开关、补丁参数、临时兜底、过渡适配层、`legacy fallback` 或 `compat mode`。
- 现有模型、接口或流程无法准确表达业务语义时，应直接重构核心结构，不得继续叠加标识位和补丁式判断。
- 应保持一条清晰的主实现路径，删除死代码、无效分支、废弃参数和重复逻辑。
- 只有在提升可读性和职责边界时才抽取私有方法、辅助组件或领域服务，不得为抽象而抽象。
