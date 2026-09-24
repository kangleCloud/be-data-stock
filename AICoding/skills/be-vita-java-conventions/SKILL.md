---
name: be-vita-java-conventions
description: 处理 be-vita 的 Java 新增、修改、重构和代码审查，覆盖模块分层、命名、事务、异常、格式和中文注释；不用于纯文档或纯 SQL 任务。
---

# be-vita Java 约定

## 执行入口

1. 读取 `AICoding/skills/common-java/SKILL.md`，执行通用 Java 工作流。
2. 应用 `AICoding/rules/be-vita-ai-codegen-rules.md` 中的“模块与依赖边界”“分层职责”“统一技术基线”“命名与目录”“持久化与 SQL”“事务、校验与异常”“核心业务中文注释”“模块专项规则”“禁止事项”和“pre-v1.0 重构原则”。

## 项目上下文

- 构建与验证从根 Maven 工程执行；具体常用命令见根目录 `README.md`。
- 涉及表结构或 Entity 字段映射时加载 `be-vita-database-design`；新增或变更表需要标准 CRUD 时加载 `be-vita-crud-generation`。

## 任务检查

实施前确认目标模块、依赖方向、公开契约、相邻实现和测试范围，识别数据库、文档及其他模块联动。

实施后按引用的主规则逐项检查模块和分层边界、仓库基础类型、事务与异常语义、关键中文注释及无关格式变化，并对受影响模块执行编译和测试。涉及动态调用关系时须用源码或可执行结果复核。
