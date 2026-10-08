# Scheduler 本机市场数据刷新 V1

仅 `vita-scheduler` 暴露八个固定 POST 入口，统一部署前缀 `/scheduler/api/local/market-data/v1`，不接收 kind 选择参数。Controller 只校验真实来源并转发；Python 同步编排和终态模型在 `vita-service` 的 `com.vita.marketdata` 中。

| 后缀 | Java 业务 / Python 内部入口 | Java 读取超时 |
| --- | --- | --- |
| `/stock/dictionary/refresh` | 股票字典 → `/internal/stock-monitor/v1/exchange-dictionary` | 90 秒 |
| `/stock/profiles/refresh` | 启用股票资料 → `/internal/stock-monitor/v1/profiles` | 90 秒 |
| `/calendar/refresh` | `/internal/jobs/v1/calendar/refresh` | 90 秒 |
| `/market/refresh` | `/internal/jobs/v1/market/refresh` | 1440 秒 |
| `/stock/quotes/refresh` | `/internal/jobs/v1/monitor/refresh` | 360 秒 |
| `/etf/dictionary/refresh` | ETF 字典 → `/internal/etf-monitor/v1/dictionary` | 60 秒 |
| `/etf/profiles/refresh` | 启用 ETF 同花顺基本资料 → `/internal/etf-monitor/v1/profiles` | 210 秒 |
| `/etf/quotes/refresh` | `/internal/jobs/v1/etf/refresh` | 360 秒 |

入口无需登录或入站令牌，只接受真实 `request.getRemoteAddr()` 为 `127.0.0.1`、`::1` 或 IPv6 全写；存在 `Forwarded`、`X-Forwarded-For`、`X-Real-IP` 即拒绝，空值也拒绝。其他来源业务码 403；非 POST 为 405。旧 scheduler 内部整体刷新入口和原本机路径全部移除，不提供别名。管理端整体刷新和每日定时 Service 调用保留。

Java 继续用 `vita.stock-monitor.python-base-url` 与 `internal-token` 直连 Python，携带 `X-Internal-Token`。配置缺失拒绝调用，真实值只在 Git 忽略的 profile YAML 中填写，不使用环境变量占位符。市场 Python 上限为 1380 秒，Java 读取预算为 1440 秒；日历/个股/ETF Python 上限分别为 60/300/120 秒。调用方需保留更长等待预算。

## 返回语义

统一使用 `CommonResult`，业务错误以 `code` 表达，外层 HTTP 仍为 200。

- 字典和资料返回各领域原有刷新结果，股票 `{accepted,status,startedAt,finishedAt,message}`，ETF `{status,startedAt,finishedAt,message}`。
- 股票 `accepted=true,status=SUCCESS` 为本次完成；`ERROR` 为本次失败；`accepted=false` 为本次未执行，携带的上次状态不能当作本次成功。独立手动字典/资料仍分别保留 10/30 分钟间隔（429），资料关闭雪球总闸时拒绝（503）。
- ETF `SUCCESS` 表示同步完成；`PARTIAL` 表示部分同花顺基本资料不可用，成功部分仍落库。整体刷新只更新字典及启用 ETF 的同花顺基本资料，重建清单缓存并发送 resync，不自动采集行情或资产配置。空启用清单不调用资料源。Java整体锁或Python资料批次锁冲突为 423；资料30分钟间隔未满足为429；源错误、无有效资料或非法响应为 503，不能假称同步成功；资料和字典不受雪球总闸影响。
- 日历、市场、股票行情、ETF 行情返回 `{kind,state,outcome,startedAt,finishedAt,message}`，终态为 `SUCCEEDED/PARTIAL/SKIPPED/FAILED`。源限频或冷却可返回 `SKIPPED`，业务失败不能当作成功；Python 409 映射 423，基础设施错误 503，读取超时 504。同步等待最终结果，不创建 jobId 或轮询队列。

行情任务仍遵守交易日、交易时段、120 秒间隔、锁、冷却和总闸规则。ETF 新浪行情与同花顺基本资料不依赖雪球，资产配置需总闸开启。公开大屏的普通刷新仅 GET 缓存，REST + SSE 自动重同步契约不变。

## 本机 curl 示例

以下在 scheduler 服务器本机执行，端口按部署配置调整；无需请求令牌，也不加转发头：

```bash
scheduler_url=http://127.0.0.1:19002/scheduler/api/local/market-data/v1
curl --max-time 400 -X POST "$scheduler_url/stock/dictionary/refresh"
curl --max-time 400 -X POST "$scheduler_url/stock/profiles/refresh"
curl --max-time 120 -X POST "$scheduler_url/calendar/refresh"
curl --max-time 1500 -X POST "$scheduler_url/market/refresh"
curl --max-time 400 -X POST "$scheduler_url/stock/quotes/refresh"
curl --max-time 400 -X POST "$scheduler_url/etf/dictionary/refresh"
curl --max-time 400 -X POST "$scheduler_url/etf/profiles/refresh"
curl --max-time 400 -X POST "$scheduler_url/etf/quotes/refresh"
```

ETF 基本资料调用只发送 `{symbols}`，不发送 asOfDate。THS 字段、逐股采集时间、删除旧表重建及部署顺序见 [V1 平台契约](market-platform-v1-contract.md#etf-同花顺基本资料切换2026-10-03)。资料批次最多十只串行、至少2秒间隔、180秒总预算，由 Python 独立资料锁与30分钟间隔控制；Java复用整体刷新锁，不占用Python锁。资产配置客户端仍保留120秒预算且只有独立入口触发。
