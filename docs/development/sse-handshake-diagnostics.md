# SSE 握手诊断

## 故障定位边界

前端 `requestStream` 的“事件流响应格式异常”首先表示 HTTP 握手不满足要求：响应不是 2xx、`Content-Type` 不是 `text/event-stream`，或浏览器无法取得 `response.body`。握手通过后才进入事件解析；因此该文案不能直接作为 `ready`／`patch` 字段错误的证据。

正常流接口由 `CommonStreamResult.success` 返回 `200 text/event-stream;charset=UTF-8`，不包装 `CommonResult`。握手失败应返回真实 HTTP 错误状态与 `application/json` 的 `CommonResult` 错误体，不能把 JSON／HTML 当作 SSE 接受。公开路径及版本字段见 [三大屏 V1 契约](market-platform-v1-contract.md)。

## 2026-10-09 匿名只读检查

此次仅各请求一次，单次最多 8 秒，无 Cookie／令牌，不调用 scheduler、Python 或数据源。检查只保留状态、响应类型及首帧字段，没有保存快照正文。

| 访问位置 | 路径 | 结果 | 响应类型／首帧 |
| --- | --- | --- | --- |
| `https://kangle.cloud` | `/stock/openapi/api/market/dashboard/snapshot` | HTTP 200，无 Location | `application/json` |
| `https://kangle.cloud` | `/stock/openapi/api/market/dashboard/stream` | HTTP 200，无 Location | `text/event-stream;charset=UTF-8`；`ready`，字段 `snapshotId` |
| 本机 openapi 19002 | `/openapi/api/market/dashboard/snapshot` | 连接被拒绝，未取得 HTTP 响应 | 当前未运行本机服务 |
| 本机 openapi 19002 | `/openapi/api/market/dashboard/stream` | 连接被拒绝，未取得 HTTP 响应 | 当前未运行本机服务 |

本次生产检查未复现截图中的握手异常，也未观察到 HTML 或重定向。当前可确认的应用原始上下文是 `/openapi/api`，外部路径多出的 `/stock` 属于部署入口；没有服务器配置证据，不能据此猜测具体代理规则或将故障归因于代理。生产探测不作为自动化测试依赖。

定向 MVC 测试复现了独立的后端异常分支：三个 SSE 端点在握手时抛出未捕获异常，原通用异常处理返回普通错误对象，在 `Accept: text/event-stream` 下触发 `No acceptable representation`，不能稳定返回约定的 JSON 错误体。通用异常处理已按 `@StreamEndpoint` 区分该请求，显式返回 HTTP 500 JSON；普通接口继续沿用原有 `CommonResult` 状态约定。该复现证明代码中存在此缺陷，但不能证明它就是截图发生时的生产根因。

## 缓存与连接生命周期

- 缓存中单个模块 `ERROR`／`STALE` 属于模块数据状态；不能直接据此判定 HTTP 握手失败。缓存键不可用、整体格式错误或读取异常是否阻断握手，须看实际 HTTP 状态与错误体。
- 通知只唤醒读取当前缓存。连续版本发送 patch，普通重复通知过滤；乱序、版本缺口、Redis 当前版本领先通知时发送 resync。同版本显式 resync 仍须通知客户端重新 GET 后建流。
- 三个流服务的 `STREAM_TIMEOUT_MS=60000`，心跳间隔 15 秒，到期调用 `SseEmitter.complete()` 并清理连接。这是正常生命周期，不发送业务错误事件。客户端按既有重连流程重新 GET 后建流，不能把正常 EOF 当成初次握手的格式错误；不增加固定 GET 轮询。
- 本机隔离 HTTP 测试使用真实 Controller、MVC、异常处理和精确匿名路径配置，业务依赖使用模拟服务，不读取本机 profile 私有值、不连接数据库或 Redis。服务层测试单独覆盖连续通知和并发读取。

## 本轮验证结果

| 范围 | 已完成验证 |
| --- | --- |
| MVC 握手与异常处理 | 12 项：三种流正常首帧、404／503 JSON、未捕获异常 HTTP 500 JSON、普通 REST 错误约定及缓存模块 ERROR 时仍可握手 |
| 生产鉴权配置的 MVC 请求链 | 5 项：三个精确匿名流、受保护流 HTTP 401、首次授权后异步完成不重复鉴权 |
| 市场／个股／ETF 服务和刷新契约 | 51 项：连续增量、重复／乱序、Redis 领先、版本缺口、聚合读取重试、同版本显式 resync、三类流 60 秒定时到期以及既有终态结果与令牌转发 |
| scheduler 本机入口 | 2 项：固定八入口、真实回环和转发头限制 |
| 编译和静态检查 | admin、app、openapi、scheduler 及其依赖编译通过；嵌套类检查与 `git diff --check` 通过 |

合计 70 项定向测试通过，不代表全仓所有测试或生产根因已经验证。上述 MVC 测试不绑定端口。

`vita-openapi/src/test/java/com/vita/controller/stream/SseHandshakeHttpTest.java` 另保留真实 Tomcat 随机端口测试，验证线上的响应头、首帧、JSON 错误及正常 EOF。此测试首次在沙箱中因端口绑定被禁止而未能启动；两次申请执行本机隔离测试的自动权限审批均超时，未取得用户授权继续执行。因此没有将这组真实本机 HTTP 测试列为通过，等待授权后再运行。生产的两次短时只读观察与本机自动化测试相互独立，也没有持有生产连接等待完整 60 秒结束。

## 若再次出现生产异常

保留浏览器 Network 中该次失败请求的时间、实际请求路径、最终 HTTP 状态、`Content-Type`、去除查询参数的重定向路径，以及对应服务日志的请求标识和状态。不要提供 Cookie、令牌、请求凭证或完整快照正文。

需结合实际部署的 JAR 版本、openapi 上游地址／端口和路径改写规则比对这一次请求。本次没有访问服务器配置、执行 SSH、修改生产 service 或部署；在这些证据缺失时，不认定代理、浏览器或模块采集中的任一项为根因。
