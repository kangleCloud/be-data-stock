# be-vita

`be-vita` 是基于 Spring Boot 3.5.0、JDK 17、Maven 多模块结构的后端仓库，当前维护管理端、App端、公开查询端、调度端、公共基础能力、业务服务层、代码生成器和多个 Spring Boot starter。

## 文档分工

- `README.md`：仓库总览、模块结构、启动入口与常用命令。
- `AGENTS.md`：面向 AI 任务的规划、Skill 路由与执行控制入口。
- `AICoding/rules/be-vita-ai-codegen-rules.md`：唯一详细且最高优先级的 AI 代码生成规则源。
- `AICoding/skills/`：可复制的 `common-*` 通用基座及注入本仓库约束的 `be-vita-*` 适配层。

## 当前模块结构

```text
be-vita
├── vita-common
├── vita-service
├── vita-workflow
├── vita-generator
├── vita-starters
│   ├── vita-captcha-spring-boot-starter
│   ├── vita-file-spring-boot-starter
│   ├── vita-i18n-spring-boot-starter
│   └── vita-poi-spring-boot-starter
├── vita-admin
├── vita-app
├── vita-openapi
├── vita-scheduler
├── AICoding
│   ├── rules
│   └── skills
├── tools
│   └── codegraph
├── sql
│   ├── init
│   └── upgrade
└── docs
    ├── requirements
    ├── architecture
    └── development
```

## 模块职责与依赖

- `vita-common`：公共基础模块，承载统一返回、异常、分页、MyBatis-Plus、Redisson、Sa-Token、工具类和基础模型，不放置工作流契约或实现。
- `vita-service`：统一业务服务层，承载管理认证、App认证与OAuth、RBAC、系统管理等非工作流业务，不放置工作流契约或实现。
- `vita-workflow`：工作流能力模块，承载端口契约、报销流程域、Warm-Flow 配置、处理器、运行时实现与资源文件，允许单向调用 `vita-service`。
- `vita-admin`：管理端启动模块，入口类 `com.vita.config.VitaAdminApplication`，上下文路径 `/admin/api`。
- `vita-app`：App端启动模块，仅保留启动类、HTTP Controller和运行配置；App认证与OAuth业务统一位于 `vita-service`。入口类 `com.vita.config.VitaAppApplication`，上下文路径 `/app/api`。
- `vita-openapi`：公开查询接口启动模块，入口类 `com.vita.config.VitaOpenApiApplication`，上下文路径 `/openapi/api`。
- `vita-scheduler`：调度任务启动模块，入口类 `com.vita.config.VitaSchedulerApplication`，上下文路径 `/scheduler/api`。
- `vita-generator`：代码生成器与模板模块，入口 `com.vita.config.CodeGenerator`。
- `vita-starters`：starter 聚合父模块，通过 `starter-*` profile 挂载各个子 starter。

主依赖链以根 `pom.xml` 为准：

```text
vita-admin   -> vita-service -> vita-common
vita-admin   -> vita-workflow -> vita-service -> vita-common
vita-app     -> vita-service -> vita-common
vita-openapi -> vita-service -> vita-common
vita-scheduler -> vita-service -> vita-common
```

## 文档与脚本入口

- 文档总导航：`docs/README.md`
- 需求与验收：`docs/requirements/`
- 系统与 Starter 设计：`docs/architecture/`
- 开发与运维指南：`docs/development/`
- AI Skill 导航：`AICoding/skills/README.md`
- 本地开发脚本：`tools/`；CodeGraph 脚本只能位于 `tools/codegraph/`。
- 系统初始化：`sql/init/system.sql`
- 初始化数据：`sql/init/insert.sql`
- App 认证初始化：`sql/init/app.sql`
- 升级脚本：`sql/upgrade/`
- 运行日志：`data/logs/be-vita/`；可通过环境变量 `VITA_LOG_HOME` 覆盖。

## 常用命令

- 编译全仓：`mvn clean compile`
- 打包全仓：`mvn clean package -DskipTests`
- 运行全仓测试：`mvn test`
- 启动管理端：`mvn -pl vita-admin -am spring-boot:run`
- 启动App端：`mvn -pl vita-app -am spring-boot:run`
- 启动公开接口：`mvn -pl vita-openapi -am spring-boot:run`
- 启动调度端：`mvn -pl vita-scheduler -am spring-boot:run`
- 执行公共模块测试：`mvn -pl vita-common -am test`
- 执行业务层测试：`mvn -pl vita-service -am test`
- 执行代码生成：先按 `docs/development/code-generator.md` 配置类中常量，再运行 `mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator`
- 执行验证码 starter 测试：`mvn -f vita-starters/pom.xml -Pstarter-captcha test`

## 可选开发工具

- CodeGraph 用于本地辅助分析跨模块调用链、接口实现、影响范围和 MyBatis XML 映射，不属于应用运行时依赖或 CI 门禁。
- 团队试点固定使用 `@colbymchenry/codegraph@1.5.0`，唯一命令入口为 `./tools/codegraph/codegraph.sh`，本地索引不提交 Git。
- 安装、Codex/Claude MCP 注册、使用限制和回滚方式见 `docs/development/codegraph-integration.md`。

## 本地启动顺序

1. 启动 MySQL 8.x 和 Redis 7.x。
2. 依次执行 `sql/init/system.sql`、`sql/init/insert.sql` 和 `sql/init/app.sql`；启用工作流时再执行 `sql/init/workflow.sql`。
3. 启动 `vita-admin`、`vita-app`、`vita-openapi` 或 `vita-scheduler`。

## 配置与协作入口

- 当前 `application.yml` 通过 `${vita.*}` 占位符读取外部化配置，请在 profile 文件或运行环境中提供对应值。
- AI 任务的规划、Skill 路由和交付流程见 `AGENTS.md`。
- 模块边界、代码、事务、DDL、生成器及文档联动的强制约束见 `AICoding/rules/be-vita-ai-codegen-rules.md`。
- 通用 Skill、be-vita 适配层和跨项目复制方式见 `AICoding/skills/README.md`。
