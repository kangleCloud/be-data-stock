# CodeGraph 接入指南

## 定位与边界

CodeGraph 是团队可选的本地代码分析工具，用于辅助理解跨模块调用链、接口实现、影响范围和 MyBatis Java/XML 映射。它不属于 Maven 依赖、应用运行时、部署组件或 CI 门禁。

接入链路为：

```text
Codex / Claude -> stdio MCP -> CodeGraph -> .codegraph/codegraph.db
```

仓库级唯一入口是 `./tools/codegraph/codegraph.sh`。人工操作和代理执行 shell 命令时不得绕过该入口；个人 MCP 配置内部直接执行官方 `codegraph serve --mcp` 是唯一例外。CodeGraph 相关脚本只能放在 `tools/codegraph/`。

仓库不提交 `.mcp.json`、`codegraph.json` 或 `.codegraph/`。个人 MCP 配置由 `setup` 子命令维护，本地 SQLite 索引由脚本固定在仓库根目录。

## 安装与初始化

脚本兼容 macOS/Linux Bash 3.2 及以上，要求本机已安装 npm。试点版本固定为 `@colbymchenry/codegraph@1.5.0`。

```bash
./tools/codegraph/codegraph.sh setup
```

`setup` 会执行以下操作：

1. 安装固定版本并关闭 CodeGraph 遥测。
2. 检测本机 Codex、Claude CLI；存在时删除并重建个人环境中名为 `codegraph` 的 MCP 配置。
3. 将 MCP 工具限制为 `codegraph_explore`，同时关闭遥测和自动更新检查。
4. 在仓库根目录初始化索引；索引已存在时执行增量同步。

`setup` 会修改个人 npm 和 MCP 配置。其他日常命令不修改个人环境。完成后需要重启已注册的代理。

## 命令入口

```bash
./tools/codegraph/codegraph.sh init
./tools/codegraph/codegraph.sh status
./tools/codegraph/codegraph.sh sync
./tools/codegraph/codegraph.sh reindex
./tools/codegraph/codegraph.sh explore "AuthController.login -> AuthServiceImpl.login"
./tools/codegraph/codegraph.sh help
```

- `init`：无索引时初始化，已有索引时同步。
- `status`：检查 CLI 版本、遥测、索引、Git 忽略和已安装代理的 MCP 配置。
- `sync`：增量同步分支切换或文件修改后的索引。
- `reindex`：索引异常时执行强制全量重建。
- `explore`：执行精确符号或调用链查询，默认最多输出 400 行。

需要临时调整查询输出上限时，仅对当前命令设置正整数：

```bash
CODEGRAPH_OUTPUT_MAX_LINES=200 \
  ./tools/codegraph/codegraph.sh explore "SysRoleMapper.selectRoleCodesByIds"
```

不得在 Maven 子模块下另建索引，也不得增加模块级 CodeGraph 脚本、别名或自定义启动入口。

## 使用规则

- 开始分析前先检查目标模块目录、技术栈、分支状态和局部约定，再使用 CodeGraph 定位调用关系。
- 查询优先提供完整类名，或使用“类名 + 方法名”，避免同名符号噪声。
- 工作流 Port/Adapter、多级接口继承、多实现类、反射和框架运行时绑定必须再用 `rg` 与源码确认。
- CodeGraph 无结果、结果含糊、索引过期或工具不可用时，立即回退到精确搜索、文件片段读取和 Maven 验证，不得阻塞开发。
- CodeGraph 输出不能代替 Java 编译、自动化测试、SQL 检查和源码事实。
- 已在 `docs/requirements/`、`docs/architecture/` 或契约文件中记录的背景，直接引用路径和章节，不在查询或会话中重复粘贴。

## 故障处理

按以下顺序处理索引或 MCP 异常：

```bash
./tools/codegraph/codegraph.sh sync
./tools/codegraph/codegraph.sh status
./tools/codegraph/codegraph.sh reindex
```

`status` 报告 MCP 缺失或启动命令失效时，重新执行 `setup` 并重启代理。npm 或 CLI 不可用时，脚本会返回非零状态和错误原因；不要通过直接调用 CodeGraph 绕过版本或根目录检查。

## 试点验收

试点持续 1 至 2 周并覆盖至少 10 个真实任务，至少验证以下查询：

- 认证链：`AuthController.login -> IAuthService.login -> AuthServiceImpl.login`
- MyBatis 链：`SysRoleMapper.selectRoleCodesByIds -> SysRoleMapper.xml`
- 工作流链：多级接口未完整解析时，代理必须明确结果不完整并回退源码检查

通过标准：目标符号定位正确率不低于 80%，不得因图谱误判产生错误修改，重复搜索与文件读取次数至少降低 30%。

## 升级与回滚

升级前必须在临时环境中复测认证、MyBatis 和工作流基准查询。通过后统一修改脚本中的固定版本、本文档和根目录摘要，不允许开发者自行漂移版本。

退出试点必须显式确认：

```bash
./tools/codegraph/codegraph.sh teardown --yes
```

该命令删除仓库根索引、同名个人 MCP 配置和全局 npm 包，但保留 `tools/codegraph/` 仓库机制。没有 `--yes` 时脚本拒绝执行。

参考资料：[安装](https://colbymchenry.github.io/codegraph/getting-started/installation/)、[配置](https://colbymchenry.github.io/codegraph/getting-started/configuration/)、[MCP Server](https://colbymchenry.github.io/codegraph/reference/mcp-server/)、[遥测说明](https://github.com/colbymchenry/codegraph/blob/main/TELEMETRY.md)。
