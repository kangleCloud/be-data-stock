# AI Skill 导航

本目录存放可复制的通用工作流和 be-vita 薄适配层。所有 Skill 使用标准 `SKILL.md` 结构，当前由仓库文档显式路由，不依赖代理原生自动发现。

## 职责分层

- `common-*`：发现目标项目约定并执行数据库、Java 或 CRUD 通用工作流，不包含 be-vita 专属规则、类型、路径或命令。
- `be-vita-*`：加载对应通用 Skill，注入本仓库规则章节、项目路径、生成器命令和任务检查项。
- `AICoding/rules/be-vita-ai-codegen-rules.md`：定义 be-vita 强制约束，不承担任务步骤编排。

be-vita 的任务选择、加载顺序和冲突优先级统一以根目录 `AGENTS.md` 为准。

## Skill 映射

| 通用 Skill | be-vita 适配层 | 适用任务 |
| --- | --- | --- |
| `common-db` | `be-vita-database-design` | DDL、迁移、SQL、Entity 映射和数据库审查 |
| `common-java` | `be-vita-java-conventions` | Java 新增、修改、重构和审查 |
| `common-crud` | `be-vita-crud-generation` | 使用既有生成器创建标准 CRUD 骨架 |

复合任务由适配层按需组合多个 Skill。通用 Skill 为支持独立复制，会保留必要的上下文发现与验证流程。

## 复制到其他项目

从仓库根目录复制通用基座：

```bash
mkdir -p /path/to/target/AICoding/skills
cp -R AICoding/skills/common-* /path/to/target/AICoding/skills/
```

目标项目应为实际使用的基座新增薄适配 Skill，注入自己的规则入口、目录、Schema 契约、生成器命令和验证方式，再由目标项目的代理指南显式路由。复制后使用 Skill 校验器检查目录结构，并搜索清除源项目专属内容。

通用基座升级采用重新复制和人工审查，不保证跨项目自动同步。
