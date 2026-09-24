# 仓库代理指南

## 文档职责

- `README.md` 维护仓库结构、模块职责、常用命令和人工导航。
- `AGENTS.md` 负责 AI 任务的规划、Skill 选择、执行控制和交付要求。
- `AICoding/rules/be-vita-ai-codegen-rules.md` 是 be-vita 唯一完整的强制规则源。
- `AICoding/skills/README.md` 说明 Skill 分层和复用方式。

规则优先级固定为“主规则 > `be-vita-*` 适配层 > `common-*` 通用流程”。执行顺序为“本文件规划 -> 适配 Skill -> 通用 Skill -> 主规则约束 -> 验证交付”。

## 规划阶段

1. 检查 `git status --short --branch`、目标目录、技术栈和相邻实现，识别并保护已有修改。
2. 涉及历史需求、缺陷或既有契约时，先检索 `docs/`、提交记录和当前实现；文档与实现冲突时必须明确说明并复核事实。
3. 明确目标、非目标、影响模块、公开契约、事实来源和风险，不仅依据需求描述或外部经验制定方案。
4. 判断需要加载的适配 Skill，并确认是否联动需求、架构、开发文档或 SQL。
5. 在实施前确定验证命令；无法执行的检查必须在交付时说明原因和风险。

## Skill 路由

| 任务类型 | be-vita 适配层 | 通用基座 |
| --- | --- | --- |
| 建表、改表、SQL、Entity 映射或 DDL 审查 | `AICoding/skills/be-vita-database-design/SKILL.md` | `common-db` |
| Java 新增、修改、重构或代码审查 | `AICoding/skills/be-vita-java-conventions/SKILL.md` | `common-java` |
| 新增或变更表后的标准 CRUD | `AICoding/skills/be-vita-crud-generation/SKILL.md` | `common-crud` |

复合任务必须加载全部相关适配 Skill；无关任务不加载。适配层负责引入通用流程和主规则对应章节，不得自行放宽硬约束。

## 执行阶段

- 先用精确文件名、类名、方法名或关键字定位，再读取必要片段；大型日志、SQL、Mapper XML、构建输出和差异必须限制范围。
- 只修改任务所需内容，保护用户已有改动，避免无关文件变化。
- 已固化在中央文档或契约中的背景直接引用文件和章节，不在会话或新文档中重复粘贴。
- CodeGraph 仅作可选辅助，使用约束与故障回退见 `docs/development/codegraph-integration.md`；源码、编译和测试结果仍是最终依据。
- 实现必须同时满足主规则和所加载 Skill 的任务检查项。

## 验证与交付

- 优先执行受影响模块的编译、测试和专项检查，并审查最终差异、文档联动及意外文件。
- 文档类变更至少执行 Skill 校验、路径或引用检查、`git diff --check` 中适用的项目。
- 交付说明必须列出修改结果、已执行验证、未执行项目及剩余风险，不得把未验证内容描述为已通过。
