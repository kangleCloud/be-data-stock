# 文档导航

仓库文档统一维护在 `docs/`，不得在 `vita-*` Maven 子模块中散落需求或详细设计。开始实现前先从本页定位已有背景，后续讨论直接引用文件路径和章节，不重复粘贴已固化内容。

## Requirements

`docs/requirements/` 保存业务目标、流程规则、验收条件和需求产物。

- 报销审批流程：`docs/requirements/workflow/reimburse-approval-v1.md`
- 报销流程定义：`docs/requirements/workflow/definitions/reimburse-approval-v1.json`

## Architecture

`docs/architecture/` 保存系统能力和 Starter 的技术方案、边界、数据流与详细设计。

- 系统设计：`docs/architecture/system/`
- App认证与OAuth设计：`docs/architecture/system/app/app-auth-oauth-design.md`
- Starter 设计：`docs/architecture/starter/`

## Development

`docs/development/` 保存实施顺序、开发操作、外部组件接入和本地工具指南。

- 代码生成器：`docs/development/code-generator.md`
- RBAC 开发指南：`docs/development/system/rbac/`
- MinIO AK/SK 指南：`docs/development/starter/file/vita-file-minio-aksk-guide.md`
- CodeGraph 接入：`docs/development/codegraph-integration.md`

## 维护规则

- 新需求先进入 `requirements`，技术方案进入 `architecture`，实施与运维说明进入 `development`。
- 文档引用使用仓库相对路径，禁止写入个人机器绝对路径。
- 修改认证、RBAC、日志、工作流、Starter 或数据库契约时，必须同步更新对应文档和 `sql/` 脚本。
