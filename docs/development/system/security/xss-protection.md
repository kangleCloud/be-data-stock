# 请求层 XSS 检测

## 注册与配置

`vita-common` 的 `com.vita.web.xss.XssConfig` 是唯一注册入口，创建一个 `FilterRegistrationBean<XssFilter>`；Filter 自身不参与组件扫描。admin、app、openapi、scheduler 均扫描 `com.vita`，各自 dev/prod 的忽略 YAML 直接配置 `vita.xss`，不使用环境变量占位符。合并片段见 [脱敏配置样例](xss-config.example.yml)，保留本机私有值及已有日志配置。

| 配置项 | 默认值 | 含义 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用过滤器注册 |
| `url-patterns` | `["/*"]` | Servlet URL 映射，支持 `/prefix/*` 等 Servlet 模式 |
| `exclude-paths` | `[]` | 移除 context-path 后精确匹配，不支持通配符、不连带放行子路径 |
| `max-json-body-size` | `10MB` | 原始 JSON 缓存和单个 multipart 文本上限，超限 HTTP 413；文件二进制不缓存 |

顺序在字符编码、Spring `FormContentFilter` 之后且在 MVC 之前，因此 PUT/PATCH/DELETE 表单也能检测。仅注册 `REQUEST`，`asyncSupported=true`；OPTIONS 跳过，ASYNC/ERROR 不重复消费请求。OPTIONS 原有 MVC／鉴权语义不由本过滤器改变。不默认排除登录或 SSE。

## 检测及原文保留

参考 [RuoYi-Vue Filter 注册](https://github.com/yangzongzhuan/RuoYi-Vue/blob/master/ruoyi-framework/src/main/java/com/ruoyi/framework/config/FilterConfig.java) 的配置 → Filter → 请求包装器结构，但项目规则为检测后拒绝，不清洗放行、不 trim、不跳过 GET。使用 jsoup **1.23.2** 与 `Cleaner(Safelist.none())` 检查解析树，见 [jsoup 官方说明](https://jsoup.org/cookbook/cleaning-html/safelist-sanitizer)。不使用标签正则或危险关键词替换。

- 检测 query/form 参数名和值，包括 GET/DELETE；JSON 使用 Jackson 流式遍历所有字符串及字段名，覆盖嵌套对象／数组和重复字段。接受 `application/json` 与 `application/*+json`。
- JSON 只用于检测，缓存原始字节后让下游读取原文；包装器支持重复 `getInputStream()`、`getReader()` 和空体，参数原样委托。中文、空格、引号、`&amp;`、比较表达式（如 `收益 < 5`）、正常 URL 不改写。
- multipart 由 Servlet 沿用既有上传大小限制，只检查普通文本 part（按声明字符集或请求编码解码）和参数，不读取文件二进制。超出既有上传限制仍拒绝，不增加文件大小额度。
- 检测到不允许的 HTML 时直接返回 **HTTP 400**、JSON `CommonResult.error(400,"请求包含不允许的 HTML 内容")`。非法 JSON 返回 400 固定格式文案。Filter 不使用 `sendError`、不依赖 ControllerAdvice，不回显输入、密码或源正文。

## 凭据豁免

以下现有接口仅豁免 **POST JSON 顶层字符串 `password`**；按实际 context-path 与精确路由双重限定。

| context-path | 路由 | 实际用途 |
| --- | --- | --- |
| `/admin/api` | `/auth/login` | 管理端登录 `AuthLoginDto.password` |
| `/admin/api` | `/system/sysUser/add` | 用户新增 `SysUserCreateDto.password` |
| `/admin/api` | `/system/sysUser/update` | 用户密码更新 `SysUserUpdateDto.password` |
| `/app/api` | `/auth/login` | App 登录 `AppLoginDto.password` |
| `/app/api` | `/auth/register` | App 注册 `AppRegisterDto.password` |

查询、表单、其他方法／路径、嵌套对象或数组中的同名字段不豁免，用户名等其他字段仍检测；不全局按字段名放行。部署若改动上述 context-path 或新增真实凭据接口，须同步审查精确映射与测试。Token 请求头不扫描，原有鉴权与权限检查保留。

## 边界与验证

本机制针对进入业务的请求输入，不改写响应、第三方资料、已存储数据或 SSE 内容，也不能覆盖存量／第三方 XSS。DTO 领域校验、菜单完整 HTTP/HTTPS URI 校验和前端实际渲染处的文本／Tooltip 转义继续保留，数据库不存储 HTML 转义文本。合法业务值不应使用 `innerHTML` 等不安全渲染方式。

定向测试覆盖危险 query/form/嵌套 JSON/multipart 文本拒绝、正常原文、凭据路径、重复读体／Reader／空体、兼容媒体类型、413、启停及精确排除。真实本地 Tomcat 随机端口测试使用模拟业务服务与隔离配置，验证 `/admin/api`、`/app/api` 密码、实际 multipart、表单、URL 映射，以及三个匿名 SSE 握手和异步完成；不连接生产 Redis、MySQL 或真实登录账号。四种 context-path 测试均检查容器内过滤器仅注册一次，不能等同于四个完整应用连接生产依赖的启动验收。
