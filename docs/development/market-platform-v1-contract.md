# 三大屏 V1 增量契约

本契约适用于市场总览、个股监控和 ETF 监控。行情采集统一经 AKShare，真实来源不得为东方财富。金额单位元，价格单位元或指数点位，涨跌幅数值单位百分数。缺失值返回 JSON `null`，不得用 0 或估算值填充。所有时间戳为带 `+08:00` 偏移的 ISO 8601；无可靠源时间时 `sourceTime:null`，日内曲线横轴为实际 `collectedAt`。

采集版本固定 AKShare `1.18.97`。Python 采集容器内存固定 1GiB，`memory=1g`、`memory-swap=1g`，不增加额外 swap；保护阈值为 800MiB。全局源进程最多 2 个，同一来源最多 2 个，不自动扩大内存。Java 保留同步等待终态、内部 Token 和固定本机入口，不新增 jobId 或补采来源。新浪 ETF 交易行情不得改用 `fund_etf_spot_ths`（基金净值）。

资金与行情使用独立通道。资金通道只调用一次 `stock_fund_flow_individual("即时")`，从同一批数据生成市场资金、涨跌家数及启用股票资金；行情通道按股票、ETF、指数、行业、概念顺序采集。每条通道两次启动至少间隔 120 秒，不重叠、不排队；慢资金轮不能阻塞行情，已完成模块及时发布。120 秒不是实际更新周期保证，慢轮及冷却可能延长更新间隔。手动市场整体刷新沿用原入口，同步取得两条通道准入；任一通道忙立即返回冲突，不持有一条通道等待另一条。其他刷新使用行情通道准入，Python 409 仍映射 Java 423。交易日期以可靠交易日信息判定，不将工作日直接等同交易日。

市场发布只合并本次变化模块，保留其他通道已发布内容；Redis `WATCH/MULTI/EXEC` 首次尝试后最多重试 3 次，合计最多 4 次，快照、版本和通知在同一事务提交，冲突耗尽不发布。股票资金、stateId 与通知继续受既有监控锁和事务保护；失败仅更新状态与诊断，不伪造资金点。Redis 键、频道、V1 GET/SSE 路径及消息结构保持不变。

## 输入与 XSS 边界

请求 DTO 按领域约束代码格式、市场枚举、长度和数量：个股允许 SH/SZ/BJ 加六位代码，ETF 允许 SH/SZ；启用标志非空，排序列表最多十只，空启用集合允许空排序列表；指数限定五个既有码且排序为 1～5。分页关键词等普通文本保留中文、`&`、`<` 和引号，不全局过滤字符串、不向数据库写入 HTML 转义结果。JSON 返回原文本，前端在实际渲染边界使用文本渲染或经审查的 HTML 清理。

菜单外链创建和更新使用完整 URI 校验，仅接受 http/https、有效主机与合法端口，拒绝危险协议、协议相对地址、控制字符（包括编码后的控制字符）及畸形地址；不设置主机白名单，合法路径、查询和片段原样保留。内部路由沿用原语义。菜单排序以数值范围 0～999 校验，不能将字符串长度约束用于 Integer。

## 加载与事件

首次进入、SSE 断流或版本缺口、页面恢复可见及普通手动刷新时 GET 完整缓存；正常更新由 SSE 推送，不固定每 10 秒 GET。普通刷新不触发 AKShare。服务器本机 scheduler 通过 [八个固定入口](python-jobs-local-api.md) 同步等待刷新结果，不生成 Java `jobId`。入口只允许真实回环直连、拒绝转发头，无需入站令牌；Java→Python 继续携带 `X-Internal-Token`。

三个 SSE 都以 `ready` 发送当前版本；版本连续时发送 `patch`，旧缓存无版本、乱序/漏事件或无法构造补丁时发送 `resync`，客户端重新 GET 后建流。版本缺失是 JSON `null`，不能序列化为字符串 `"null"`。断线重连应先 GET，再建立 SSE；`ready` 版本与 GET 不同则再 GET。

成功握手固定返回 `200 text/event-stream;charset=UTF-8`，不包装 `CommonResult`；握手业务异常返回真实 HTTP 错误状态和 JSON，未捕获异常返回 HTTP 500 JSON。连接约 60 秒主动完成属于正常 EOF，心跳每 15 秒发送；正常 EOF 不属于 HTTP 握手响应格式错误。部署排查步骤及只读检查证据见 [SSE 握手诊断](sse-handshake-diagnostics.md)。

通知只用于唤醒，补丁内容读取 Redis 当前快照。当前版本已领先通知或通知基版本与连接版本不一致时，发送 `resync`，不拼接跨版本补丁。同版本的普通重复通知可忽略，显式 `resync:true` 必须发送 `resync`；进入 resync 状态的连接不再发送 patch，客户端重新 GET 后重新建流。个股和 ETF 全量聚合读取校验读取前后的状态版本；首次变化重试一次，持续变化返回 503，避免返回混合版本的数据。

| 看板 | 完整 GET | SSE | Redis 版本与通知 |
| --- | --- | --- | --- |
| 市场 | `/openapi/api/market/dashboard/snapshot` | `/openapi/api/market/dashboard/stream` | `stock:market:v1:snapshot`、`stock:market:v1:updates` |
| 个股 | `/openapi/api/stock-monitor/v1/dashboard` | `/openapi/api/stock-monitor/v1/stream` | `stock:monitor:v1:state-id`、`stock:monitor:v1:updates` |
| ETF | `/openapi/api/etf-monitor/v1/dashboard` | `/openapi/api/etf-monitor/v1/stream` | `stock:etf-monitor:v1:snapshot`、`stock:etf-monitor:v1:state-id`、`stock:etf-monitor:v1:updates` |

这些路径仅精确放行 GET；管理端需登录；scheduler 八个本机入口免登录但严格限制回环直连。三个看板维持 `schemaVersion:1`。个股与 ETF 各自最多启用 10 只，不共享额度。

## 市场 `coreIndices`

市场 `modules` 在现有 `industrySectors`、`conceptSectors`、`marketFundFlow` 外新增 `coreIndices`。外壳沿用 `{status,tradeDate,tradeDateBasis,lastSuccessAt,lastAttemptAt,message,data}`。失败或冷却时，有可复用的最近成功数据则保留原 `data`、交易日期及成功时间，状态降为 `STALE` 并更新尝试时间与诊断；没有可复用数据时为 `ERROR`、`data:null`，其他模块仍可用。指数源为 AKShare `stock_zh_index_spot_sina` 的新浪数据，固定代码及顺序为 `sh000001` 上证、`sz399001` 深证成指、`sh000300` 沪深300、`sz399006` 创业板、`sh000688` 科创50；管理员可禁用及排序，代码不从 ETF 名称推测。

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

## 股票资金状态

股票公开对象在既有 `fundSeries` 后增加 `fundFlowStatus` 与可空字符串 `fundFlowMessage`，不修改报价、价格曲线或 V1 版本字段。

| fundFlowStatus | 含义 |
| --- | --- |
| `AVAILABLE` | 卡片有效交易日为当前日期，有该日有效资金点，且当前 `marketFundFlow` 为同日 FRESH |
| `STALE` | 有该卡片日期的保留资金点，但模块失败／冷却／资源不足，模块日期不一致，或资金点属于历史日期 |
| `NO_DATA` | 该卡片有效交易日没有有效资金点；不能用其他日期的点补满或将空值填零 |
| `DISABLED` | 公开采集总闸关闭，资金点不展示 |

Java 从 `stock:monitor:v1:enabled` 读取规范化启用清单，仅读取 `stock:monitor:v1:fund-series:{effectiveTradeDate}:{symbol}` 的真实点。有效日期优先报价／价格曲线，缺失时才取资金日期；同一卡片两条曲线保持同日。重复时间或不满足 `netAmount=inflow-outflow` 的曲线作为无有效采样处理，不能选取重复冲突的后一条。

状态诊断读取对应市场模块的真实 `status/tradeDate/lastAttemptAt/message`，输出固定中文分类与合法的最近尝试时间，不透传源异常正文；AVAILABLE 时 message 为 null。同批 `DUPLICATE_CONFLICT` 由 Python 标记资金失败并保留旧点，Java 不另请求逐股源。资金模块失败或冷却等状态变化，即使没有新资金点，Python 也须沿既有监控事务推进股票 stateId 并通知启用股票，以使 SSE 的状态／诊断变化可达；不能伪造资金点。GET 与股票 patch 都包含新增状态字段，ready 仍只有 stateId、resync 仍为空对象。

## ETF 缓存与公开视图

ETF 字典来自 AKShare `fund_etf_category_sina("ETF基金")` 的新浪行，含可靠 `sh`/`sz` 前缀，可规范化为 `SH510050` / `SZ159919`，保留六位 `code`。同源交易价不得用基金净值替代；逐条源时间不可靠，`quote.sourceTime:null`。ETF 基本资料使用 `fund_info_ths`，不再采集交易所资料；THS 资料不建立上市状态、上市日期、份额或份额日期，成功同步后这些旧字段为空，不把成立日期当作上市日期。跟踪指数代码只能来自可靠来源或经管理员核实，未知时为 `null`。雪球 `fund_individual_detail_hold_xq` 返回的是**资产配置类别占比**，不是成分股持仓；`requestedReportPeriod` 只表示请求报告期，不代表已核实披露日。

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
    "trackingIndexCode":null,"trackingIndexName":null,"updatedAt":null,
    "fullName":null,"fundType":null,"investmentType":null,"fundManager":null,
    "establishedDate":null,"performanceBenchmark":null,"source":null},
  "quote":{"source":"SINA_ETF","tradeDate":"2026-09-30","price":2.5,
    "change":0.01,"changePercent":0.4,"previousClose":2.49,"open":2.49,
    "high":2.51,"low":2.48,"volume":1000000,"amount":2500000,
    "sourceTime":null,"collectedAt":"2026-09-30T10:00:00+08:00","status":"FRESH"},
  "series":[{"collectedAt":"2026-09-30T10:00:00+08:00","price":2.5}],
  "fundSeries":[],"fundFlowStatus":"NO_RELIABLE_SOURCE",
  "effectiveTradeDate":"2026-09-30","dataStatus":"CURRENT","closeConfirmed":false,
  "assetAllocation":null,"assetAllocationStatus":"NOT_SYNCED"
}
```

`effectiveTradeDate` 优先取最近有效行情/价格曲线日期，再取资金曲线日期；两条曲线只返回该同一日期的真实点，缺样各自为空，不补点。`dataStatus` 为 `CURRENT`、`DELAYED`、`HISTORICAL` 或 `NO_DATA`；无可靠源时间时 `closeConfirmed=false`，历史报价不能声称已确认收盘。ETF 资金净流入在无可靠非东财来源时固定 `fundSeries:[]`、`fundFlowStatus:"NO_RELIABLE_SOURCE"`，页面显示“暂无可靠数据”。雪球总闸 `xqEnabled=false` 时隐藏 `assetAllocation`，但不隐藏新浪交易行情与同花顺基本资料。

`assetAllocation` 非空时仅含 `{requestedReportPeriod,source:"XQ_DANJUAN",collectedAt,categories:[{category,percent}]}`，没有成分股、持仓日期或虚构的披露日期。新增 `assetAllocationStatus`：总闸关闭优先 `DISABLED` 并隐藏报告；开启且有有效报告为 `AVAILABLE`；缺失或无效报告为 `NOT_SYNCED`。有效报告要求正确来源、报告期、采集时间和非空有效类别百分比。

该报告快照存既有 MySQL 报告表，行情/曲线只存 Redis。ETF SSE `ready:{stateId}`、`patch:{baseStateId,stateId,etfs:[仅变化 symbol 的行情公开对象]}`、`resync:{}`；行情 patch 保留 `assetAllocationStatus`，不重复携带完整 `assetAllocation`。报告同步成功推进既有 resync，完整报告由重新 GET 读取。ready／resync 字段不变。ETF 资金仍为 `fundSeries:[]`、`fundFlowStatus:"NO_RELIABLE_SOURCE"`。

## 内部同步与管理端

Python 内部服务仅供带 `X-Internal-Token` 的 Java 调用：

| 内部路径 | 请求 | 响应要点 |
| --- | --- | --- |
| `POST /internal/etf-monitor/v1/dictionary` | `{}` | `{schemaVersion:1,source:"SINA",etfs:[{symbol,code,name,market,exchange,etfType,listingStatus,listingDate,trackingIndexCode,trackingIndexName}]}` |
| `POST /internal/etf-monitor/v1/profiles` | `{symbols:[...]}`，不接收 asOfDate | `{schemaVersion:1,source:"THS",collectedAt,profiles:[{symbol,code,source:"THS",collectedAt,fullName,fundType,investmentType,fundManager,establishedDate,performanceBenchmark,manager,custodian}],sourceStatus:{symbol:"OK/ERROR/SKIPPED"}}` |
| `POST /internal/etf-monitor/v1/asset-allocation` | `{symbol,reportPeriod:"YYYYMMDD"}` | `{schemaVersion:1,symbol,requestedReportPeriod:"YYYY-MM-DD",source:"XQ_DANJUAN",collectedAt,categories:[{category,percent}]}`；仅雪球总闸开启时调用 |

管理端路径以 `/admin/api` 为前缀，均需登录与精确权限：

| 路径 | 权限 | 用途 |
| --- | --- | --- |
| `GET /system/etfDictionary/page` | `system:etf-dictionary:view` | `pageNum/pageSize/keyword/market/etfType`，返回 `PageResponse`，行含 symbol/code/name/market/exchange/etfType/listingStatus/listingDate/trackingIndexCode/trackingIndexName/source/syncedAt |
| `GET /system/etfProfile/page` | `system:etf-profile:view` | `pageNum/pageSize/keyword/fundType/trackingIndexCode`；分页行含 symbol/code/name/market、资料字段和带偏移的 updatedAt |
| `GET /system/etfProfile/detail?symbol=...` | `system:etf-profile:view` | `{dictionary,profile,assetAllocation}`；有权限管理员可看已有报告，不受总闸关闭遮蔽；公开端仍隐藏 |
| `GET /system/etfMonitor/list` | `system:etf-monitor:view` | 已启用清单，最多 10 条 |
| `POST /system/etfMonitor/enable` | `system:etf-monitor:update` | `{symbol,enabled}`；不影响个股 10 只额度 |
| `POST /system/etfMonitor/sort` | `system:etf-monitor:update` | `{symbols:[...]}`；必须与当前已启用集合一致 |
| `POST /system/etfMonitor/refresh` | `system:etf-monitor:refresh` | 同步等待字典与同花顺基本资料，返回 `{status,startedAt,finishedAt,message}`，无 jobId |
| `POST /system/etfMonitor/allocation/refresh?symbol=...&reportPeriod=YYYYMMDD` | `system:etf-monitor:refresh` | 雪球总闸开启且 ETF 已启用时同步实际类别占比 |

资产配置复用上述入口（实际管理端前缀 `/admin/api`），选择请求报告期，同步等待结果；成功按 symbol＋报告期插入或更新原报告表并发 resync，失败保留旧报告。不另建接口，不由 ETF 整体刷新自动触发。

Python 失败只增加 `detail.reason` 分类 `RESOURCE/NO_DATA/DISABLED/SOURCE`，成功包不变。NO_DATA 仅表示确实无该报告期有效数据，格式变化／任意解析异常不能归为无报告。Java 不透传 detail.message 或源正文；旧字符串 detail 和未知分类使用固定通用诊断。

| Python HTTP／本地校验 | Java 业务码 | 诊断 |
| --- | --- | --- |
| 409 通道／任务忙 | 423 | 立即返回冲突，当前任务完成后可重试 |
| 429 限频 | 429 | 限频或冷却 |
| 422 参数／Java 无效日期或未启用 ETF | 400 | 代码或报告期不合法／只能同步已启用 ETF |
| 503 RESOURCE | 503 | 资源不足，旧报告保留 |
| 502 NO_DATA | 503 | 没有该请求报告期有效报告，旧报告保留 |
| 503 DISABLED／Java 总闸关闭 | 503 | 总闸或授权关闭；Java 关闭时零 Python 调用 |
| 502 SOURCE／其他失败 | 503 | 源不可用／通用同步失败，旧报告保留 |

管理端 HTTP 状态与业务码沿普通 `CommonResult` 约定，不能将 Python 409 混同于请求成功；不输出 Token 或源正文。
| `GET /system/indexConfig/list` | `system:index-config:view` | 五指数配置 code/name/enabled/sortOrder |
| `POST /system/indexConfig/update` | `system:index-config:update` | `{code,enabled,sortOrder}`；代码仅限五个已核实默认值 |

MySQL 仅存 ETF 字典、监控配置、基础资料、实际取得的资产配置报告快照，以及五指数启停和顺序；不存实时价格及曲线。字典、资料缺源字段可空。初始化 SQL、增量升级与资料表重建脚本必须使用仓库公共审计字段并保持一致。登录和权限查询不得读取匿名报价；普通页面刷新不得触发内部同步。


## Java 业务结构与整体刷新结果

`vita-service` 的市场、个股、ETF 服务分别位于 `com.vita.marketdata.market`、`stockmonitor`、`etfmonitor` 子包。共用时区、独立清单上限与 SSE 参数在 `MarketDataConstants`，Redis V1 字面值在各领域常量类；配置绑定仍为 `vita.stock-monitor`，共用锁与 Python 同步编排在总业务包。`CoreIndexEnum` 定义五个代码/名称，数据库继续决定启停和顺序。包迁移不改变公开 GET/SSE、Redis DB/键/频道或消息字段。

ETF `POST /admin/api/system/etfMonitor/refresh` 与每日定时任务复用同一服务，只同步字典和启用 ETF 的同花顺基本资料、重建缓存并通知 resync，不自动采集资产配置或行情。结果仍为 `{status,startedAt,finishedAt,message}`：`SUCCESS` 为完成，`PARTIAL` 为部分基金资料缺失或被限频跳过；刷新锁冲突业务码 423，源错误及非法响应业务码 503，异常不伪装成功。雪球关闭不影响该整体刷新。独立资产配置入口继续受总闸控制。

个股整体刷新结果字段不变，`accepted=false` 表示本次未执行，不能将携带的上次 `SUCCESS/ERROR` 当作本次结果；`accepted=true,status=ERROR` 表示本次失败。前端整体刷新同步等待，不新增 jobId 或状态轮询。完整 scheduler 映射、超时及 curl 见上述本机刷新契约。


## ETF 同花顺基本资料切换（2026-10-03）

生产基本资料只使用固定 AKShare 1.18.97 的 `fund_info_ths`。不再采集 SSE/SZSE 基本资料，不回退雪球基本资料或东财。字典与交易行情仍使用新浪；雪球资产配置入口及总闸独立保留。基本资料不受 `xqEnabled` 控制，不进入 120 秒行情采样。

公开 profile、资料分页、详情 profile 均新增可空 `fullName/fundType/investmentType/fundManager/establishedDate/performanceBenchmark/source`。`manager` 为基金管理人，`fundManager` 为基金经理；`establishedDate` 为成立日，不能写入 `listingDate`。`performanceBenchmark` 保留基准原文，不推断 `trackingIndexCode`。`fundType` 为同花顺基金类型，与字典 `etfType` 分类独立，资料页筛选改用 `fundType`。详情通过 DTO 显式返回 `updatedAt`，不透出内部 `profileUpdatedAt`。

每股成功记录 `source=THS`，`updatedAt` 来自该 profile 的实际 `collectedAt`，不是批次完成时间或报价源时间。成功切换时显式清空 `listingStatus/listingDate/shareCount/shareDate`；MP 默认忽略 null 的更新不得用于该替换。交易所标识、ETF 分类和已核实跟踪指数仅来自字典，不从基准推断。用户最新要求为删除旧表重新构建：重建脚本删除 `etf_monitor_profile` 全部资料（包含已采集的 THS 数据），按新结构建空表，之后通过资料刷新重新采集，不保留历史标识。重建后的已成功 THS 资料在后续失败或跳过时不改内容及采集时间。

`sourceStatus` 必须精确覆盖请求 symbol，只允许 `OK/ERROR/SKIPPED`；仅 `OK` 且代码、THS 来源、字段及采集时间合法的资料落库。全部有效为 `SUCCESS`，部分有效为 `PARTIAL` 并保留失败项；无有效资料明确返回业务失败（503），不发布本次资料 resync。空启用清单为无需采集的成功。成功资料写入后用既有 resync 推进版本，GET/SSE 路径、schemaVersion 1、快照 Redis 键与通知契约不变。

Java 读取预算：字典 60 秒、资料 210 秒、资产配置 120 秒；前端整体刷新 300 秒。Python 最多十只串行、源请求间隔至少 2 秒、总预算 180 秒，超预算标 SKIPPED；每股 30 分钟间隔由 Python 保证。Python 独占资料批次锁 `stock:etf-monitor:v1:profiles:python:lock`（210 秒）和 `stock:etf-monitor:v1:profiles:python:min-interval:{symbol}`；Java 继续持有原 `refresh:lock` 和短期 `config:lock`，不与 Python 共用同一把锁。Python 409 映射 423、429 保持 429、无有效资料或上游错误为 503。

部署须先备份并按流程执行 [ETF 资料表重建 SQL](../../sql/upgrade/20261003_etf_profile_ths.sql)，再启动匹配的新应用版本；新 Entity 会查询新增列，不能先在旧 schema 上启动新版本。初始化与首版建表 SQL 已同步七个可空业务列；该脚本执行 `DROP TABLE IF EXISTS etf_monitor_profile` 后重新建表，表内全部原记录及审计信息会删除，重复执行也会清空后续取得的资料，不能作为保留数据的增量迁移。本次开发未执行真实数据库迁移或部署验证。
