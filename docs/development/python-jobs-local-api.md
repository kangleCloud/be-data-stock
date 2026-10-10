# Scheduler 本机市场数据刷新 V1

仅 `vita-scheduler` 暴露八个固定 POST 入口，统一部署前缀 `/scheduler/api/local/market-data/v1`，不接收 kind 选择参数。Controller 校验真实来源后固定选择 `CollectionMode.MANUAL`；Python 同步编排和终态模型在 `vita-service` 的 `com.vita.marketdata` 中。

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

## 单次模式与准入隔离

Java→Python 同时发送 `X-Collection-Mode:auto/manual`；Python 先鉴权，再解析该头，缺省为 `auto`，无效值 HTTP 400。Java 仅用独立 `CollectionMode` 枚举明确传递本次模式，不保存共享可变开关或 ThreadLocal。股票／ETF 的 `refreshDictionary(mode)`、`refreshProfiles(mode)` 和 `PythonJobsService.refresh(kind,mode)` 对应上表八项；无模式方法及整体 `refresh()` 保持 AUTO，admin 和每日定时任务继续使用这些默认方法。

八个回环入口忽略外来 `X-Collection-Mode`，由服务端固定选择 MANUAL；浏览器或调用者不能通过该头升级 admin/timer，也不能将本机入口降为 AUTO。真实回环与转发头校验继续先于业务调用。curl 无需发送模式头。

MANUAL 跳过 Java 采集刷新锁、字典 10 分钟／股票资料 30 分钟触发间隔。短 CONFIG_LOCK、MySQL 事务、启用上限、源字段校验、资料保留／更新、缓存重建及 resync 继续共用原业务链路。股票字典与资料批次在短锁内以事务提交，源请求不占用短写锁；提交成功后才发布缓存变化。本次手动结果只在同步响应中返回，不能覆盖 admin/timer 共用 RUNNING 或终态记录；`refresh/status` 仍表达 AUTO 状态。

Python MANUAL 跳过普通失败冷却、任务触发间隔、任务／入口锁及源并发配额；仍执行共享实际 HTTP 限速、401/403/429 与明确风控保护、800MiB 内存保护、超时和进程回收。MANUAL 允许休市／时段外尝试，但不得伪造交易日期或曲线点。上述源端行为由 Python 实现，Java 只传模式并复用同步返回／错误处理，不另建 jobId、轮询或 Redis 业务键。

## 返回语义

统一使用 `CommonResult`，业务错误以 `code` 表达，外层 HTTP 仍为 200。

- 字典和资料返回各领域原有刷新结果，股票 `{accepted,status,startedAt,finishedAt,message}`，ETF `{status,startedAt,finishedAt,message}`。
- 股票 `accepted=true,status=SUCCESS` 为本次完成；`ERROR` 为本次失败；`accepted=false` 为 AUTO 本次未执行，携带的上次状态不能当作本次成功。AUTO 独立字典／资料仍分别保留 10/30 分钟间隔（429），本机 MANUAL 不占用该间隔。两种模式下资料关闭雪球总闸均拒绝（503）。
- ETF `SUCCESS` 表示同步完成；`PARTIAL` 表示部分同花顺基本资料不可用，成功部分仍落库。整体刷新只更新字典及启用 ETF 的同花顺基本资料，重建清单缓存并发送 resync，不自动采集行情或资产配置。空启用清单不调用资料源。AUTO 刷新锁／Python 资料批次锁冲突为 423、资料 30 分钟间隔未满足为 429；本机 MANUAL 跳过这些任务准入，但不绕过写锁和实际 HTTP／风控保护。源错误、无有效资料或非法响应为 503，不能假称同步成功；资料和字典不受雪球总闸影响。
- 日历、市场、股票行情、ETF 行情返回 `{kind,state,outcome,startedAt,finishedAt,message}`，终态为 `SUCCEEDED/PARTIAL/SKIPPED/FAILED`。源限频或冷却可返回 `SKIPPED`，业务失败不能当作成功；Python 409 映射 423，基础设施错误 503，读取超时 504。同步等待最终结果，不创建 jobId 或轮询队列。

AUTO 行情任务仍遵守交易日、交易时段、120 秒间隔、锁、冷却和总闸规则。AUTO 市场整体刷新同步取得资金与行情两条通道准入，任一忙则立即返回 423，不排队；其他 AUTO 刷新按行情通道准入规则执行。本机 MANUAL 按上述隔离规则准入。容器 1GiB、800MiB 保护阈值、AUTO 全局及同源进程各最多 2 个，以及增量事务发布规则统一见 [V1 平台契约](market-platform-v1-contract.md)。ETF 新浪行情与同花顺基本资料不依赖雪球，资产配置需总闸开启。公开大屏的普通刷新仅 GET 缓存，REST + SSE 自动重同步契约不变。

## 本机 curl 示例

以下在 scheduler 服务器本机执行，端口按部署配置调整；无需请求令牌或模式头，也不加转发头。服务端固定 MANUAL，跳过任务准入限制后仍可能因实际源限速、风控、资源或写锁失败：

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

ETF 基本资料调用只发送 `{symbols}`，不发送 asOfDate；模式放在请求头中，不修改成功包。THS 字段、逐股采集时间、删除旧表重建及部署顺序见 [V1 平台契约](market-platform-v1-contract.md#etf-同花顺基本资料切换2026-10-03)。资料批次最多十只串行、实际 HTTP 间隔至少 2 秒、180 秒总预算；AUTO 由 Python 独立资料锁与 30 分钟间隔控制，Java 复用整体刷新锁。本机 MANUAL 跳过任务锁和触发间隔，写入保护及实际 HTTP 限速仍生效。资产配置客户端仍保留 120 秒预算且只有独立 AUTO 入口触发。
