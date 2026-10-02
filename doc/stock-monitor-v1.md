# 个股监控大屏 V1 跨端契约

## 范围与来源

系统级监控清单最多启用 10 只股票。MySQL `stock_monitor_config.enabled` 是每只股票是否进入监控及采集清单的唯一配置；YAML 的 `vita.stock-monitor.xq-enabled` 仅是默认关闭的雪球授权总闸，不改变各股票的 `enabled`。MySQL 保存交易所股票字典、监控启停和排序、选中股票的有限基础资料；Redis DB 2 保存生效清单、雪球报价和当日真实采样曲线。交易所字典独立同步；选中股票资料、报价只来自雪球。总闸关闭时任何自动或手动整体刷新均不得请求雪球，匿名接口不得输出旧的雪球报价。授权未确认期间只用模拟雪球数据联调。

Spring Boot 的 admin、scheduler、openapi 启动模块在各自 `application-dev.yml`、`application-prod.yml` 中直接填写 `vita.stock-monitor.python-base-url`、`internal-token`、`xq-enabled`，再由 `StockMonitorProperty` 统一绑定。开发环境的 Python 地址默认 `http://127.0.0.1:8000`；生产地址和令牌在部署机器的本机 YAML 中填写；生产雪球开关默认 `false`。`application-*.yml` 不使用环境变量占位符；`application*.yml` 为本机忽略配置，可跟踪脱敏模板见 `config/stock-monitor-dev.example.yml` 和 `config/stock-monitor-prod.example.yml`，不得在模板中填写真实令牌。

股票标识统一为 `SH600000`、`SZ000001`、`BJ920262` 格式。时间戳为带 `+08:00` 偏移的 ISO 8601，交易日为 `YYYY-MM-DD`；金额和价格单位元，成交量 `volume` 单位股，`previousClose` 为雪球“昨收”（元），个股卡片显示为“昨收 · 元”、保留两位小数，缺失显示 `—`；涨跌幅数值单位百分数，例如 `1.23` 表示上涨 1.23%。缺失数值返回 `null`，不得填 0。状态只使用 `DISABLED`、`FRESH`、`STALE`、`ERROR`。

## Redis DB 2

| 键 | 值 | 写入者 |
| --- | --- | --- |
| `stock:monitor:v1:enabled` | 按 `sortOrder` 升序排列的 `[{"symbol":"SH600000","code":"600000","name":"浦发银行","market":"SH"}]` JSON，最多 10 条 | Java 在启动预热和管理端变更后重建 |
| `stock:monitor:v1:quote:{symbol}` | `{"schemaVersion":1,"symbol":"SH600000","source":"XQ","sourceTime":"2026-09-28T14:30:00+08:00","collectedAt":"2026-09-28T14:30:03+08:00","tradeDate":"2026-09-28","price":10.2,"changePercent":1.23,"amount":1234567.89,"low":10.05,"high":10.31,"open":10.11,"limitUp":11.0,"limitDown":9.0,"averagePrice":10.18,"volume":121274,"previousClose":10.08,"status":"FRESH"}` | Python 采集端，仅雪球开关开启时写入 |
| `stock:monitor:v1:series:{tradeDate}:{symbol}` | `[{"time":"2026-09-28T09:31:00+08:00","price":10.2}]`，按真实源时间升序 | Python 采集端，仅雪球开关开启时写入 |
| `stock:monitor:v1:fund-series:{tradeDate}:{symbol}` | `[{"collectedAt":"2026-09-28T09:31:00+08:00","inflow":100000,"outflow":40000,"netAmount":60000}]`，同批个股资金采集，单位元 | Python 市场采集端 |
| `stock:monitor:v1:lastTradeDate` | `YYYY-MM-DD` | Python 采集端 |
| `stock:monitor:v1:state-id` | 每次个股报价、曲线或资金点发布后的唯一版本 ID | Python 采集端；Java 清单/资料变更时也推进版本 |

曲线只追加实际源时间点，同一时间覆盖去重，不插值、不补点；午休和缺测在页面断线。仅保留最近两个有数据交易日曲线。Redis 丢失后公开页返回不可用状态，不从 MySQL 恢复价格、涨跌幅或曲线。关闭雪球开关时 Java 公开页忽略所有报价与曲线键。旧 V1 报价未包含 `low/high/open/limitUp/limitDown/averagePrice/volume/previousClose` 时，这些字段返回 `null`；非数值或非有限数值视为无效报价。盘中 Python 对启用股票每 120 秒采样一次；页面以首次 GET 和 SSE 增量更新，不再定时 GET。

## HTTP 包装和公开接口

Java 接口使用项目 `CommonResult<T>`：`{"code":200,"success":true,"msg":"接口调用成功","content":...}`。失败使用现有错误码和 `success:false`。OpenAPI 仅精确放行 `GET /openapi/api/stock-monitor/v1/dashboard` 与 `GET /openapi/api/stock-monitor/v1/stream`，其他路径仍需登录。

`GET /openapi/api/stock-monitor/v1/dashboard` 无参数。`content`：

```json
{
  "schemaVersion": 1,
  "stateId": null,
  "xqEnabled": false,
  "tradeDate": null,
  "stocks": [
    {
      "symbol": "SH600000",
      "code": "600000",
      "name": "浦发银行",
      "market": "SH",
      "sortOrder": 1,
      "profile": {"industry": null, "listingDate": null, "marketCap": null, "updatedAt": null},
      "quote": {"source": "XQ", "sourceTime": null, "collectedAt": null, "tradeDate": null, "price": null, "changePercent": null, "amount": null, "low": null, "high": null, "open": null, "limitUp": null, "limitDown": null, "averagePrice": null, "volume": null, "previousClose": null, "status": "DISABLED"},
      "series": [],
      "effectiveTradeDate": null,
      "dataStatus": "DISABLED",
      "closeConfirmed": false,
      "fundSeries": []
    }
  ]
}
```

`stocks` 始终按生效顺序排列且最多 10 条。`stateId` 与 Redis 版本键一致，旧缓存可为 JSON `null`。开关关闭时 `xqEnabled=false`，`tradeDate=null`，`profile`、`quote` 的雪球字段隐藏，`series=[]`、`fundSeries=[]`、`dataStatus=DISABLED`；即使仍有旧缓存也不输出。每股 `effectiveTradeDate` 优先取最近有效报价或价格曲线的日期，只有没有价格时才取资金曲线日期；两条曲线只读取该同一日期的键，缺失时各自为空。`dataStatus` 为 `CURRENT`、`DELAYED`、`HISTORICAL`、`NO_DATA` 或 `DISABLED`。`closeConfirmed` 仅在有效报价的实际源日期等于该报价交易日、上海时间严格晚于 15:00:00 时为真，历史交易日的真实收盘也可为真。`profile` 仅包含已同步的有限基础资料。

SSE `GET /openapi/api/stock-monitor/v1/stream` 建立后先发 `ready:{stateId}`；Python 的 `stock:monitor:v1:updates` 通知含 `{baseStateId,stateId,changedSymbols}`。版本连续时发送 `patch:{baseStateId,stateId,stocks:[...]}`，其中只含变更 symbol 的完整公开股票对象；清单、排序、资料变化或版本缺口发送 `resync`，客户端重新 GET 全量后建流。匿名页面手动刷新只 GET 缓存，不触发采集；首次加载及断线重同步也使用 GET。
Redis 的 enabled 键缺失、重复 symbol 或其他格式错误返回业务 503；只有显式 JSON `[]` 表示尚未启用股票。Redis 丢失不能从 MySQL 伪造匿名页的股票列表或报价。

## 管理端接口

所有路径带 `/admin/api` 前缀，需登录及 `@SaCheckPermission`。列表及字典搜索均只返回基础字段，不返回雪球报价。

| 方法和路径 | 权限 | 请求 | `content` |
| --- | --- | --- | --- |
| `GET /system/stockMonitor/page` | `system:stock-monitor:view` | `pageNum=1&pageSize=10&keyword=浦发&enabled=false`；关键词匹配代码或名称，启用状态可选 | `PageResponse<AdminStock>`；从配置表分页，包含停用记录，行含 `symbol/code/name/market/enabled/sortOrder/profile` |
| `GET /system/stockDictionary/page` | `system:stock-dictionary:view` | `pageNum=1&pageSize=10&keyword=600000&market=SH`；交易所可选 | `PageResponse<DictionaryItem>`；行含 `symbol/code/name/market` |
| `POST /system/stockDictionary/add` | `system:stock-dictionary:add` | `{"market":"SH","code":"600000","name":"浦发银行"}` | `DictionaryItem`；服务端生成 `symbol`，重复代码拒绝，不自动启用监控 |
| `GET /system/stockProfile/page` | `system:stock-profile:view` | `pageNum=1&pageSize=10&keyword=浦发&industry=银行`；行业可选 | `PageResponse<ProfileStock>`；行含 `symbol/code/name/market/industry/listingDate/marketCap/updatedAt` |
| `GET /system/stockMonitor/dictionary?keyword=浦发&limit=20` | `system:stock-monitor:view` | `keyword` 可为名称或代码，`limit` 1–50 | `[{symbol,code,name,market}]` |
| `GET /system/stockMonitor/list` | `system:stock-monitor:view` | 无 | 当前启用的至多 10 条 `[{symbol,code,name,market,enabled,sortOrder,profile:{industry,listingDate,marketCap,updatedAt}}]`，其中 `enabled=true`；停用后从清单消失，可从字典搜索重新启用 |
| `POST /system/stockMonitor/enable` | `system:stock-monitor:update` | `{"symbol":"SH600000","enabled":true}` | `true`；启用超过 10 只返回业务参数错误 |
| `POST /system/stockMonitor/sort` | `system:stock-monitor:update` | `{"symbols":["SH600000","SZ000001"]}`，必须与当前启用集合一致 | `true` |
| `POST /system/stockMonitor/refresh` | `system:stock-monitor:refresh` | `{}` | `{accepted,jobId,status}`；触发与 scheduler 相同的整体刷新，锁冲突返回 `accepted:false` |
| `GET /system/stockMonitor/refresh/status` | `system:stock-monitor:view` | 无 | `{jobId,status,startedAt,finishedAt,message}` |

三个 `page` 接口均返回 `CommonResult<PageResponse<T>>`，`PageResponse` 的 `list` 是当前页，`total` 是当前筛选条件下的 MySQL 总数；`PageRequest` 默认每页 10 条，最多 100 条。监控页的已启用数取现有 `GET /system/stockMonitor/list` 的长度（最多 10），不从分页 `total` 推断。监控页启停继续调用 `enable`，排序继续提交当前完整启用集合；停用记录保留在配置表，可再次启用。三个分页接口不调用 Python 或其他第三方；资料页只展示 MySQL 已同步值，`updatedAt` 是上次资料同步时间，即使雪球总闸关闭也保留历史值。三个菜单分别授权，普通角色由现有角色授权流程分配。
手工新增仅允许 `SH/SZ/BJ` 与六位代码、非空且不超过 100 字的名称；同 `symbol` 后续出现在交易所同步结果中时，以交易所返回值覆盖手工名称，源站未返回的手工记录保留。

## 调度接口与 Python 内部接口

`POST /scheduler/api/internal/stock-monitor/v1/refresh` 为受保护的整体刷新入口，空 JSON 请求，返回与管理端刷新相同的任务状态。每日任务在中国时区收盘后触发同一服务方法；Redis 锁 `stock:monitor:v1:refresh:lock` 防并发，任务状态键 `stock:monitor:v1:refresh:status` 保存上述状态，重复触发不并行。刷新先同步交易所股票字典，再对最多 10 只启用股票同步 profile。开关关闭时不得向 Python 提交任何雪球采集请求。

供服务器本机 `curl` 的两个独立手动入口为 `POST /scheduler/api/local/stock-monitor/v1/dictionary/refresh` 和 `POST /scheduler/api/local/stock-monitor/v1/profiles/refresh`，均无需登录和请求令牌，但只接受 TCP 直连来源 `127.0.0.1` 或 `::1`，携带 `Forwarded`、`X-Forwarded-For` 或 `X-Real-IP` 的请求会被拒绝；网关不得转发外部请求到这两个路径。字典入口只同步交易所并重建 Redis 清单，资料入口只同步当前已启用股票资料；资料总闸关闭时返回 503 且不访问雪球。两入口与定时刷新共用锁和状态，分别有 10 分钟、30 分钟的重触发间隔；缓存重建和资料落库还与管理端启停共用配置锁。现有带令牌整体刷新入口不变。Java 到 Python 的内部调用继续携带 `X-Internal-Token`。

Java 调用 Python 内部端点（服务地址由 `vita.stock-monitor.python-base-url` 配置）：

| 方法和路径 | 请求 | 响应 |
| --- | --- | --- |
| `POST /internal/stock-monitor/v1/exchange-dictionary` | `{}` | `{"schemaVersion":1,"stocks":[{"symbol":"SH600000","code":"600000","name":"浦发银行","market":"SH"}]}`；仅交易所清单，无雪球调用 |
| `POST /internal/stock-monitor/v1/profiles` | `{"symbols":["SH600000"]}`，最多 10 条 | `{"schemaVersion":1,"profiles":[{"symbol":"SH600000","industry":null,"listingDate":null,"marketCap":null,"updatedAt":"2026-09-28T15:30:00+08:00"}]}`；仅雪球开关开启时调用 |

Python 报价采集独立按照 Redis 契约写键。交易日上海时间 15:02、15:04、15:06、15:08、15:10 仅对尚未确认收盘的启用股票重试，每股请求至少间隔 120 秒。只有源日期为当日且实际源时间严格晚于 15:00:00，才追加收盘点并停止该股重试；源时间不晚于 15:00:00 时不追加收盘点，保留有效报价与曲线并标记现有 `STALE`，15:10 截止仍未确认也不新增状态字段或伪收盘点。休市、总闸关闭、冷却或清单为空时不请求雪球。Java 整体刷新不直接采集报价；在开关关闭时 Python 自身的自动调度也必须停止雪球访问。内部请求头为 `X-Internal-Token`，Java 在本机 YAML 的 `vita.stock-monitor.internal-token` 中填写与 Python 相同的令牌；令牌不得出现在公开响应或日志。`profiles` 在 Python 开关关闭时也必须拒绝并保持零雪球请求。

## MySQL 边界

`stock_symbol_dictionary` 只保存交易所 `symbol/code/name/market`；`stock_monitor_config` 保存系统级选中股票的 `enabled/sort_order`；`stock_monitor_profile` 仅保存选中股票的 `industry/listing_date/market_cap/updated_at`。报价、涨跌幅、成交额和分时曲线不得写入 MySQL。管理变更提交后重建 Redis enabled 清单；服务启动只从 MySQL 预热此清单，不发起第三方请求。
