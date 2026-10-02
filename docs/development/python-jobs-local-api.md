# Scheduler 本机 Python 手动任务 V1

仅 `vita-scheduler` 暴露以下三个精确 POST 路径，路径前缀包含部署的 `/scheduler/api`：

| 本机入口 | Python 内部入口 | 读取超时 |
| --- | --- | --- |
| `/scheduler/api/local/python-jobs/v1/calendar/refresh` | `/internal/jobs/v1/calendar/refresh` | 90 秒 |
| `/scheduler/api/local/python-jobs/v1/market/refresh` | `/internal/jobs/v1/market/refresh` | 1320 秒 |
| `/scheduler/api/local/python-jobs/v1/monitor/refresh` | `/internal/jobs/v1/monitor/refresh` | 360 秒 |

本机入口无需登录或浏览器令牌，但控制器按真实 `request.getRemoteAddr()` 只接受 `127.0.0.1`、`::1`（含 IPv6 全写）；携带 `Forwarded`、`X-Forwarded-For`、`X-Real-IP` 的请求直接拒绝。网关不得转发公网请求到这些路径。其他 scheduler 路径继续按原有鉴权执行，已有字典、资料和带令牌整体刷新路径不变。

Java 用 `vita.stock-monitor.python-base-url` 与 `internal-token` 直连 Python，每次携带 `X-Internal-Token`；配置缺失时拒绝调用。配置只在被 Git 忽略的本机 `application-dev.yml` / `application-prod.yml` 中填写真实值，不使用环境变量占位符。Java 等待 Python 本次任务的终态，不创建任务 ID、状态轮询或第二套队列。市场最坏 1260 秒、日历 60 秒、个股 300 秒，因此本机调用方也须设置足够长的等待时间，尤其市场任务建议高于 1320 秒。

成功调用使用项目 `CommonResult<PythonRunResult>` 返回，`content` 为 `{kind,state,outcome,startedAt,finishedAt,message}`。Python 终态为 `SUCCEEDED`、`PARTIAL`、`SKIPPED` 或 `FAILED`；业务失败、源限频和冷却仍是 Python HTTP 200 的终态内容，例如 `SKIPPED/outcome=throttled|cooldown`，Java 原样返回，不误报采集成功。同类任务正在执行时，Python HTTP 409 映射为业务码 423；Python 基础设施错误映射 503；连接读超时映射 504；非本机来源映射 403。项目普通业务异常的 HTTP 外层仍为 200，以 `CommonResult.code` 表达错误。

这三个入口是服务器本机手动采集操作。Python 仍执行交易日、交易时段、分布式锁、请求间隔、源冷却和雪球总闸校验；Java 不绕过这些约束。公开大屏的手动刷新仅重新 GET Redis 缓存，不调用这些采集入口；市场和个股 GET/SSE 契约分别见 `market-snapshot-v1.md`、`doc/stock-monitor-v1.md`。
