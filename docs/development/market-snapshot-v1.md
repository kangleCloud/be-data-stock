# 市场快照 V1 读取与联调

`vita-openapi` 从 Redis DB 2 的 `stock:market:v1:snapshot` 读取普通 UTF-8 JSON，订阅 `stock:market:v1:updates` 后重新读取快照。采集端契约见 `be-data-analysis/docs/market-snapshot-v1.md`。采集只使用 AKShare 的 `stock_fund_flow_industry("即时")`、`stock_fund_flow_concept("即时")`、`stock_fund_flow_individual("即时")`。快照保持 `schemaVersion=1`、`provider="akshare"`、`generatedAt`，新增每次发布唯一的 `snapshotId`；旧缓存缺失时 GET 明确返回 `snapshotId:null`。`modules` 必须恰好包含 `industrySectors`、`conceptSectors`、`marketFundFlow`。旧榜单和东财字段不作为新数据兼容源。

三个模块沿用 `{status,tradeDate,tradeDateBasis,lastSuccessAt,lastAttemptAt,message,data}` 包装。`status` 只允许 `FRESH`、`STALE`、`ERROR`。同花顺三组均无可靠源交易日期或源时间，`tradeDateBasis` 固定为 `CALENDAR`；`lastSuccessAt` 与数据中的 `collectedAt` 表示实际采集时间，不能称为源站时间。本轮失败时保留最近一次成功的 `data` 并标 `STALE`；从未成功则 `ERROR` 且 `data=null`。成功数据必须带有效交易日期及采集时间。无快照返回业务 404，结构、状态或数值无效的快照返回业务 503。

行业和概念模块的数据分别为 `{source:"THS",period:"INTRADAY",items:[...]}`，`items` 是完整板块列表。每个 item 为 `{code,name,type,indexValue,changePct,inflow,outflow,netAmount,netFlowRate,companyCount,leader,leaderChangePct,leaderPrice}`；`type` 分别为 `industry`、`concept`。无真实代码时 `code=null`，其他缺失的可空字段也为 `null`，不得填造零值。`companyCount` 必须是整数 JSON（如 `105`，不接受 `105.0`）。金额单位元；百分数数值 `2.31` 表示 `2.31%`。`netFlowRate=netAmount/(inflow+outflow)*100`，分母无效时为 `null`，不得称其为主力净占比。页面排行从完整 `items` 过滤无效值后计算：涨幅正值降序前 5、跌幅负值升序前 5、净流入正净额降序前 10、净流出负净额升序前 10。

市场模块的数据为 `{source:"THS_INDIVIDUAL_AGGREGATE",reconciledFromLegacy,latest:{collectedAt,inflow,outflow,netAmount,riseCount,fallCount,flatCount,stockCount},series:[{collectedAt,inflow,outflow,netAmount}]}`。`latest` 和每个 `series` 点的 `netAmount` 必须等于同批 `inflow-outflow`（元）；`riseCount+fallCount+flatCount` 必须等于 `stockCount`。旧缓存净额不符时 Java 读取时纠正并返回 `reconciledFromLegacy:true`，新采集为 `false`；流入、流出缺失或计数不符返回业务 503。`series` 仅包含同日实际成功采样点，时间严格递增，不补午间或失败点；只有本轮成功才追加点。报价与曲线仍只放 Redis，不写 MySQL。

快照最小示例（实际 `items` 为完整列表）：

```json
{
  "schemaVersion": 1,
  "snapshotId": "0123456789abcdef0123456789abcdef",
  "provider": "akshare",
  "generatedAt": "2026-09-30T10:02:00+08:00",
  "modules": {
    "industrySectors": {
      "status": "FRESH", "tradeDate": "2026-09-30", "tradeDateBasis": "CALENDAR",
      "lastSuccessAt": "2026-09-30T10:02:00+08:00", "lastAttemptAt": "2026-09-30T10:02:00+08:00", "message": null,
      "data": {"source": "THS", "period": "INTRADAY", "items": [
        {"code": null, "name": "银行", "type": "industry", "indexValue": null, "changePct": 2.31,
         "inflow": 1000000, "outflow": 700000, "netAmount": 300000, "netFlowRate": 17.65,
         "companyCount": 42, "leader": "浦发银行", "leaderChangePct": 3.1, "leaderPrice": 10.2}
      ]}
    },
    "conceptSectors": {
      "status": "ERROR", "tradeDate": null, "tradeDateBasis": "CALENDAR",
      "lastSuccessAt": null, "lastAttemptAt": "2026-09-30T10:02:00+08:00", "message": "采集失败", "data": null
    },
    "marketFundFlow": {
      "status": "FRESH", "tradeDate": "2026-09-30", "tradeDateBasis": "CALENDAR",
      "lastSuccessAt": "2026-09-30T10:02:00+08:00", "lastAttemptAt": "2026-09-30T10:02:00+08:00", "message": null,
      "data": {"source": "THS_INDIVIDUAL_AGGREGATE", "reconciledFromLegacy": false,
        "latest": {"collectedAt": "2026-09-30T10:02:00+08:00", "inflow": 1000000, "outflow": 700000,
                   "netAmount": 300000, "riseCount": 2500, "fallCount": 2100, "flatCount": 100, "stockCount": 4700},
        "series": [{"collectedAt": "2026-09-30T10:02:00+08:00", "inflow": 1000000,
                    "outflow": 700000, "netAmount": 300000}]}
    }
  }
}
```

公开接口为 `GET /openapi/api/market/dashboard/snapshot` 和 `GET /openapi/api/market/dashboard/stream`。仅这两个精确路径在 `vita-openapi` 应用中免登录；其他 OpenAPI 路径继续执行既有认证，`vita-admin` 不再暴露旧的 `/admin/api/market/dashboard/*`。GET 成功时以 `CommonResult.content` 返回完整快照。SSE 建立后先发 `ready:{snapshotId}`，不重复完整快照；收到 Redis `{schemaVersion:1,snapshotId,previousSnapshotId,changedModules}` 后，版本连续才发 `patch:{baseSnapshotId,snapshotId,generatedAt,modules}`，其中 `modules` 仅包含变更模块的完整对象。版本缺口、旧缓存无版本或通知与当前快照不一致时发 `resync`，客户端重新 GET 后建流。`changedModules=[]` 时可发空 `modules` patch 推进版本。请求路径只读取缓存，不同步调用 Python 或 AKShare。模块 `FRESH` 只表示本轮采集成功，页面应分别显示 `tradeDate` 和 `tradeDateBasis`，不能把历史交易日数据标成当日实时数据。

新库的 `sql/init/insert.sql` 不再写入旧管理端行情菜单与权限。已有库执行 `sql/upgrade/20260925_remove_admin_market_dashboard.sql` 清理旧菜单、权限和角色关联；脚本按业务键定位且可重复执行。若 `Market` 根菜单已有其他子菜单，脚本保留该根菜单。

本地联调由人工启动 Redis、后端和前端，并手动执行采集命令；本仓库不配置定时任务。`vita-openapi/src/main/resources/application.yml` 的默认 profile 为 `prod`，本地启动须显式指定 `dev`，例如 Maven 参数 `-Dspring-boot.run.profiles=dev`。启动前核对本地 MySQL、Redis 地址和凭据；不要连接生产 Redis。
