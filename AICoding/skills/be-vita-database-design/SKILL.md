---
name: be-vita-database-design
description: 处理 be-vita 的建表、改表、初始化或升级 SQL、Entity 字段映射和 DDL 审查；不用于不涉及数据库结构的普通代码修改。
---

# be-vita 数据库设计

## 执行入口

1. 读取 `AICoding/skills/common-db/SKILL.md`，执行通用数据库工作流。
2. 应用 `AICoding/rules/be-vita-ai-codegen-rules.md` 中的“持久化与 SQL”“文档联动”“新建表默认字段 DDL 规范”和“禁止事项”。

## 项目上下文

- 使用 MySQL，并按任务确认 `sql/init/`、`sql/upgrade/`、Entity 和中央文档中的权威来源。
- 表类型、公共字段、字段顺序、默认值及例外只以主规则“新建表默认字段 DDL 规范”为准。
- 认证、RBAC、日志等系统表变更须检查 `docs/architecture/`、`docs/development/` 和相关 SQL 是否联动。

## 任务检查

实施前确认目标表类型、受影响 Schema 来源、Entity 映射、文档和是否需要标准 CRUD；涉及 Java 或 CRUD 时分别加载对应 be-vita 适配 Skill。

实施后检查 DDL 与主规则约束、SQL 与 Entity 字段、初始化与升级脚本、设计文档是否一致，并验证语法、差异范围和例外说明。无法执行数据库验证时必须明确报告。
