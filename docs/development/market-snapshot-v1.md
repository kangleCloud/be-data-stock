# 市场快照 V1 读取与联调

`be-vita` 管理端从 Redis DB 2 的 `stock:market:v1:snapshot` 读取普通 UTF-8 JSON。采集端契约见 `be-data-analysis/docs/market-snapshot-v1.md`。服务使用字符串模板读取，校验 `schemaVersion=1`、五个模块和基础数据类型后，以 `CommonResult.content` 原样返回 JSON 树。

管理端接口为 `GET /admin/api/market/dashboard/snapshot`，需要登录及 `market:dashboard:view` 权限。无快照返回业务 404；格式或版本不合法返回业务 503，前端需展示空状态。模块 `FRESH` 仅表示本次采集成功，页面应同时展示 `tradeDate` 和 `tradeDateBasis`，不能把历史日资金流标成当日实时数据。

新库执行 `sql/init/` 脚本即可取得 `Market`／`MarketOverview` 菜单。已有库在历史行情清理脚本之后执行 `sql/upgrade/20260923_market_dashboard_snapshot.sql`。该脚本可重复执行，默认将菜单和只读权限关联到 `SUPER_ADMIN`、`COMMON_USER`；其他角色可在现有 RBAC 页面中授权。脚本不恢复旧股票监控菜单。

本地联调由人工启动 Redis、后端和前端，并手动执行采集命令；本仓库不配置定时任务。`vita-admin/src/main/resources/application.yml` 的默认 profile 为 `prod`，本地启动必须显式指定 `dev`，例如在 Maven 启动参数中设置 `-Dspring-boot.run.profiles=dev`。启动前核对 `application-dev.yml` 的本地 MySQL、Redis 地址和凭据。
