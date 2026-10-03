# 三大屏 V1 增量契约

本契约适用于市场总览、个股监控和 ETF 监控。行情采集统一经 AKShare，真实来源不得为东方财富。金额单位元，价格单位元或指数点位，涨跌幅数值单位百分数。缺失值返回 JSON `null`，不得用 0 或估算值填充。所有时间戳为带 `+08:00` 偏移的 ISO 8601；无可靠源时间时 `sourceTime:null`，日内曲线横轴为实际 `collectedAt`。

## 加载与事件

首次进入、SSE 断流或版本缺口、页面恢复可见及普通手动刷新时 GET 完整缓存；正常更新由 SSE 推送，不固定每 10 秒 GET。普通刷新不触发 AKShare。服务器本机 scheduler `/scheduler/api/local/python-jobs/v1/{market|monitor|etf}/refresh` 才同步等待 Python 重采终态，不生成 Java `jobId`，仍要求 loopback 与 `X-Internal-Token`。

三个 SSE 都以 `ready` 发送当前版本；版本连续时发送 `patch`，旧缓存无版本、乱序/漏事件或无法构造补丁时发送 `resync`，客户端重新 GET 后建流。版本缺失是 JSON `null`，不能序列化为字符串 `"null"`。断线重连应先 GET，再建立 SSE；`ready` 版本与 GET 不同则再 GET。

| 看板 | 完整 GET | SSE | Redis 版本与通知 |
| --- | --- | --- | --- |
| 市场 | `/openapi/api/market/dashboard/snapshot` | `/openapi/api/market/dashboard/stream` | `stock:market:v1:snapshot`、`stock:market:v1:updates` |
| 个股 | `/openapi/api/stock-monitor/v1/dashboard` | `/openapi/api/stock-monitor/v1/stream` | `stock:monitor:v1:state-id`、`stock:monitor:v1:updates` |
| ETF | `/openapi/api/etf-monitor/v1/dashboard` | `/openapi/api/etf-monitor/v1/stream` | `stock:etf-monitor:v1:snapshot`、`stock:etf-monitor:v1:state-id`、`stock:etf-monitor:v1:updates` |

这些路径仅精确放行 GET；管理端和 scheduler 入口均不匿名。三个看板维持 `schemaVersion:1`。个股与 ETF 各自最多启用 10 只，不共享额度。

## 市场 `coreIndices`

市场 `modules` 在现有 `industrySectors`、`conceptSectors`、`marketFundFlow` 外新增 `coreIndices`。外壳沿用 `{status,tradeDate,tradeDateBasis,lastSuccessAt,lastAttemptAt,message,data}`，模块错误时 `data:null`，其他模块仍可用。指数源为 AKShare `stock_zh_index_spot_sina` 的新浪数据，固定代码及顺序为 `sh000001` 上证、`sz399001` 深证成指、`sh000300` 沪深300、`sz399006` 创业板、`sh000688` 科创50；管理员可禁用及排序，代码不从 ETF 名称推测。

```json
{
  "source": "SINA_INDEX",
  "sourceTime": null,
  "items": [{
    "code": "sh000001", "name": "上证指数", "price": 3000.12,
    "change": 5.12, "changePercent": 0.17,
    "previousClose": 2995, "open": 2996, "high": 3001, "low": 2990,
    "volume": 1000000, "amount": 123456789,
    "sourceTime": null, "collectedAt": "2026-09-30T10:00:00+08:00",
    "series": [{"collectedAt":"2026-09-30T10:00:00+08:00","price":3000.12}]
  }]
}
```

Python 的 Redis 原始 `items` 固定包含五只已验证指数；Java 公开 GET 和 SSE patch 按 MySQL 配置过滤并排序，只返回已启用的指数。配置变化推进公开版本并发 `resync`，客户端重新 GET。失败或缺项按模块或单卡降级。五指数约每 120 秒尝试采样；分页串行、上一轮重叠跳过、限频与冷却由 Python 保证。`sourceTime` 在源未提供可靠时间前固定为 `null`，`collectedAt` 仅代表采集时间。指数不展示所谓“指数资金净流入”。市场总资金 `latest` 与每个 `series` 点的 `netAmount=inflow-outflow`，`riseCount+fallCount+flatCount=stockCount`；旧缓存纠偏只在新采集经验证上线后移除。

市场通知为 `{schemaVersion:1,snapshotId,previousSnapshotId,changedModules}`，`changedModules` 可包含 `coreIndices`。SSE `ready:{snapshotId}`、`patch:{baseSnapshotId,snapshotId,generatedAt,modules:{仅变化模块的完整对象}}`、`resync:{}`。

## ETF 缓存与公开视图

ETF 字典来自 AKShare `fund_etf_category_sina("ETF基金")` 的新浪行，含可靠 `sh`/`sz` 前缀，可规范化为 `SH510050` / `SZ159919`，保留六位 `code`。同源交易价不得用基金净值替代；逐条源时间不可靠，`quote.sourceTime:null`。交易所份额资料保留实际数据日期。跟踪指数代码只能来自可靠来源或经管理员核实，未知时为 `null`。雪球 `fund_individual_detail_hold_xq` 返回的是**资产配置类别占比**，不是成分股持仓；`requestedReportPeriod` 只表示请求报告期，不代表已核实披露日。

Python 在 Redis DB 2 写 `stock:etf-monitor:v1:snapshot`：

```json
{
  "schemaVersion": 1, "stateId": null,
  "generatedAt": "2026-09-30T10:00:00+08:00", "tradeDate": "2026-09-30",
  "items": [{
    "symbol": "SH510050", "code": "510050", "name": "50ETF", "market": "SH",
    "quote": {"source":"SINA_ETF","tradeDate":"2026-09-30","price":2.5,
      "change":0.01,"changePercent":0.4,"previousClose":2.49,"open":2.49,
      "high":2.51,"low":2.48,"volume":1000000,"amount":2500000,
      "sourceTime":null,"collectedAt":"2026-09-30T10:00:00+08:00","status":"FRESH"},
    "priceSeries": [{"collectedAt":"2026-09-30T10:00:00+08:00","price":2.5}],
    "fundSeries": []
  }]
}
```

新采集批次 `stateId` 为 32 位小写十六进制，旧缓存可为 `null`。Python 发布 `{baseStateId,stateId,changedSymbols}` 至 `stock:etf-monitor:v1:updates`。Java 管理端修改清单、顺序或资料时推进版本并发布 `resync:true`；版本键、业务快照和通知须按一次原子发布读取。Java 还写 `stock:etf-monitor:v1:enabled`，格式为按 `sortOrder` 排列的 `[{symbol,code,name,market}]`，仅包含已启用的最多 10 只。ETF 与个股状态链互不混用。

公开 GET `content` 为 `{schemaVersion:1,stateId,xqEnabled,tradeDate,etfs:[...]}`。每个 ETF 对象：

```json
{
  "symbol":"SH510050","code":"510050","name":"50ETF","market":"SH","sortOrder":1,
  "profile":{"exchange":null,"etfType":null,"listingStatus":null,"listingDate":null,
    "manager":null,"custodian":null,"shareCount":null,"shareDate":null,
    "trackingIndexCode":null,"trackingIndexName":null,"updatedAt":null},
  "quote":{"source":"SINA_ETF","tradeDate":"2026-09-30","price":2.5,
    "change":0.01,"changePercent":0.4,"previousClose":2.49,"open":2.49,
    "high":2.51,"low":2.48,"volume":1000000,"amount":2500000,
    "sourceTime":null,"collectedAt":"2026-09-30T10:00:00+08:00","status":"FRESH"},
  "series":[{"collectedAt":"2026-09-30T10:00:00+08:00","price":2.5}],
  "fundSeries":[],"fundFlowStatus":"NO_RELIABLE_SOURCE",
  "effectiveTradeDate":"2026-09-30","dataStatus":"CURRENT","closeConfirmed":false,
  "assetAllocation":null
}
```

`effectiveTradeDate` 优先取最近有效行情/价格曲线日期，再取资金曲线日期；两条曲线只返回该同一日期的真实点，缺样各自为空，不补点。`dataStatus` 为 `CURRENT`、`DELAYED`、`HISTORICAL` 或 `NO_DATA`；无可靠源时间时 `closeConfirmed=false`，历史报价不能声称已确认收盘。ETF 资金净流入在无可靠非东财来源时固定 `fundSeries:[]`、`fundFlowStatus:"NO_RELIABLE_SOURCE"`，页面显示“暂无可靠数据”。雪球总闸 `xqEnabled=false` 时隐藏 `assetAllocation`，但不隐藏新浪交易行情与交易所资料。

`assetAllocation` 非空时仅含 `{requestedReportPeriod,source:"XQ_DANJUAN",collectedAt,categories:[{category,percent}]}`，没有成分股、持仓日期或虚构的披露日期。该报告快照存 MySQL，行情/曲线只存 Redis。ETF SSE `ready:{stateId}`、`patch:{baseStateId,stateId,etfs:[仅变化 symbol 的完整公开对象]}`、`resync:{}`；配置/资料变化和不能确保增量时使用 `resync`。

## 内部同步与管理端

Python 内部服务仅供带 `X-Internal-Token` 的 Java 调用：

| 内部路径 | 请求 | 响应要点 |
| --- | --- | --- |
| `POST /internal/etf-monitor/v1/dictionary` | `{}` | `{schemaVersion:1,source:"SINA",etfs:[{symbol,code,name,market,exchange,etfType,listingStatus,listingDate,trackingIndexCode,trackingIndexName}]}` |
| `POST /internal/etf-monitor/v1/profiles` | `{symbols:[...],asOfDate:"YYYYMMDD"}` | `{schemaVersion:1,profiles:[{symbol,code,name,exchange,etfType,shareCount,shareDate,manager,custodian,listingDate,trackingIndexCode,trackingIndexName}]}` |
| `POST /internal/etf-monitor/v1/asset-allocation` | `{symbol,reportPeriod:"YYYYMMDD"}` | `{schemaVersion:1,symbol,requestedReportPeriod:"YYYY-MM-DD",source:"XQ_DANJUAN",collectedAt,categories:[{category,percent}]}`；仅雪球总闸开启时调用 |

管理端路径以 `/admin/api` 为前缀，均需登录与精确权限：

| 路径 | 权限 | 用途 |
| --- | --- | --- |
| `GET /system/etfDictionary/page` | `system:etf-dictionary:view` | `pageNum/pageSize/keyword/market/etfType`，返回 `PageResponse`，行含 symbol/code/name/market/exchange/etfType/listingStatus/listingDate/trackingIndexCode/trackingIndexName/source/syncedAt |
| `GET /system/etfProfile/page` | `system:etf-profile:view` | `pageNum/pageSize/keyword/etfType/trackingIndexCode`；分页行含 symbol/code/name/market、资料字段和带偏移的 updatedAt |
| `GET /system/etfProfile/detail?symbol=...` | `system:etf-profile:view` | `{dictionary,profile,assetAllocation}`；资产配置解析为 categories 数组和带偏移 collectedAt；雪球关时为 null |
| `GET /system/etfMonitor/list` | `system:etf-monitor:view` | 已启用清单，最多 10 条 |
| `POST /system/etfMonitor/enable` | `system:etf-monitor:update` | `{symbol,enabled}`；不影响个股 10 只额度 |
| `POST /system/etfMonitor/sort` | `system:etf-monitor:update` | `{symbols:[...]}`；必须与当前已启用集合一致 |
| `POST /system/etfMonitor/refresh` | `system:etf-monitor:refresh` | 同步等待字典与交易所资料，返回 `{status,startedAt,finishedAt,message}`，无 jobId |
| `POST /system/etfMonitor/allocation/refresh?symbol=...&reportPeriod=YYYYMMDD` | `system:etf-monitor:refresh` | 雪球总闸开启且 ETF 已启用时同步实际类别占比 |
| `GET /system/indexConfig/list` | `system:index-config:view` | 五指数配置 code/name/enabled/sortOrder |
| `POST /system/indexConfig/update` | `system:index-config:update` | `{code,enabled,sortOrder}`；代码仅限五个已核实默认值 |

MySQL 仅存 ETF 字典、监控配置、基础资料、实际取得的资产配置报告快照，以及五指数启停和顺序；不存实时价格及曲线。字典、资料缺源字段可空。初始化 SQL 与幂等升级 SQL 必须使用仓库公共审计字段并保持一致。登录和权限查询不得读取匿名报价；普通页面刷新不得触发内部同步。
