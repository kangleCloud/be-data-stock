---
name: be-vita-crud-generation
description: 为 be-vita 新增或变更的仓库自建表生成标准 CRUD 骨架，并基于 vita-generator 输出做业务改造；不用于无需标准 CRUD、纯关联表或第三方内置表。
---

# be-vita CRUD 生成

## 执行入口

1. 读取 `AICoding/skills/common-crud/SKILL.md`，执行通用 CRUD 脚手架工作流。
2. 加载 `AICoding/skills/be-vita-database-design/SKILL.md` 和 `AICoding/skills/be-vita-java-conventions/SKILL.md`。
3. 应用 `AICoding/rules/be-vita-ai-codegen-rules.md` 中的“标准 CRUD 与 vita-generator”“新建表默认字段 DDL 规范”“模块与依赖边界”“分层职责”“统一技术基线”和“禁止事项”。

## 项目入口

模板来源固定为 `vita-generator/src/main/resources/templates/mybatis-plus/`，完整配置和输出契约见 `docs/development/code-generator.md`。生成前只允许在 `CodeGenerator` 顶部常量区调整连接、表名和目标模块；真实密码不得提交，默认不得开启文件覆盖。

在仓库根目录执行：

```bash
mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator
```

## 任务检查

生成前记录工作树状态，确认目标表符合主规则适用条件，并核对生成器常量、输出模块、预期文件和可能冲突的已有修改。目标文件已存在时必须先审查差异，禁止为了通过生成而直接开启覆盖。

生成后审查生成差异，再按数据库和 Java 适配 Skill 完成必要业务改造；完成 `keyword` 查询占位，按场景补充权限和校验，检查意外文件、SQL 与文档联动，并对受影响模块执行编译和测试。交付前必须把 `DB_PASSWORD` 恢复为空、把覆盖开关恢复为 `false`。未执行生成器或验证时必须明确说明，不得将手写近似骨架描述为模板输出。
