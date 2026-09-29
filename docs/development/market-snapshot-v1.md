# 市场快照 V1 读取与联调

`vita-openapi` 从 Redis DB 2 的 `stock:market:v1:snapshot` 读取普通 UTF-8 JSON，订阅 `stock:market:v1:updates` 后重新读取快照。采集端契约见 `be-data-analysis/docs/market-snapshot-v1.md`。服务校验 `schemaVersion=1`、`provider=akshare` 和 `industryTop5`、`conceptTop5`、`marketFundFlow` 三个模块；Top5 使用同花顺盘中净额口径，资金净额字段 `netFlowAmount` 单位为元。`marketFundFlow` 仍是东方财富主力净流入语义。

公开接口为 `GET /openapi/api/market/dashboard/snapshot` 和 `GET /openapi/api/market/dashboard/stream`。仅这两个精确路径在 `vita-openapi` 应用中免登录；其他 OpenAPI 路径继续执行既有认证，`vita-admin` 不再暴露旧的 `/admin/api/market/dashboard/*`。GET 成功时以 `CommonResult.content` 原样返回完整快照；流连接建立后及 Redis 更新通知后发送 `event: snapshot`，`data` 为完整快照 JSON。无快照返回业务 404，格式或版本错误返回业务 503。模块 `FRESH` 只表示本轮采集成功，页面应分别显示 `tradeDate` 和 `tradeDateBasis`，不能把历史交易日数据标成当日实时数据。

新库的 `sql/init/insert.sql` 不再写入旧管理端行情菜单与权限。已有库执行 `sql/upgrade/20260925_remove_admin_market_dashboard.sql` 清理旧菜单、权限和角色关联；脚本按业务键定位且可重复执行。若 `Market` 根菜单已有其他子菜单，脚本保留该根菜单。

本地联调由人工启动 Redis、后端和前端，并手动执行采集命令；本仓库不配置定时任务。`vita-openapi/src/main/resources/application.yml` 的默认 profile 为 `prod`，本地启动须显式指定 `dev`，例如 Maven 参数 `-Dspring-boot.run.profiles=dev`。启动前核对本地 MySQL、Redis 地址和凭据；不要连接生产 Redis。
