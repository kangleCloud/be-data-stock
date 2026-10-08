# 系统日志开发设计文档

## 1. 文档目标

基于项目当前已有日志结构，完善系统日志设计，明确 AOP、过滤器/拦截器、上下文清理和 `sysOperLog` 落库职责。

本次设计坚持三个原则：

- 轻量化日志优先，只保留排障和审计必需信息
- 复用现有日志结构和现有 `sysOperLog` 模块，不改表结构
- 将“日志采集”和“上下文清理”拆开，避免职责混乱

## 2. 项目现状

当前仓库已经具备以下基础能力：

- `vita-admin`、`vita-openapi`、`vita-scheduler` 通过各 profile YAML 的 `logging.config` 选择日志配置。
- dev 默认 `classpath:logback-spring.xml`：控制台及普通、错误、访问三类文件日志，目录为启动工作目录下的 `data/logs/be-vita/`。
- prod 默认公共模块的 `classpath:logback-console.xml`：仅控制台，由 systemd service 指定的日志配置接管。
- 日志模式由配置值决定，不自动识别本地或 JAR 启动；`VITA_LOG_HOME` 不再用于应用日志配置。
- `vita-common/src/main/java/com/vita/log/aspect/LogAspect.java` 已存在切面骨架，已引入：
  - `@Around`
  - `MDC`
  - `NamedThreadLocal`
  - `LoginUserInfoModelContext`
- `vita-common/src/main/java/com/vita/nnotation/Log.java` 已存在 `@Log` 注解
- `vita-service` 已存在完整的 `sysOperLog` 模块：
  - 实体：`com.vita.system.sysOperLog.entity.sysOperLog`
  - DTO：`com.vita.system.sysOperLog.dto.sysOperLogCreateDto`
  - Service：`com.vita.system.sysOperLog.service.IsysOperLogService`
- `vita-common/src/main/java/com/vita/auth/context/LoginUserInfoModelContext.java` 已使用 `ThreadLocal` 保存登录用户上下文

## 3. 当前问题

结合现有代码，当前日志实现仍有几个明显缺口：

1. `logback-spring.xml` 输出的是 `%X{requestId}`，但 `LogAspect` 当前写入 MDC 的键是 `LOG_ID`，链路标识不一致。
2. `LogAspect` 目前只完成了部分准备动作，尚未真正完成注解读取、结果处理、异常捕获、`sysOperLog` 构造和落库。
3. `LoginUserInfoModelContext`、MDC、切面内部 `ThreadLocal` 的清理职责还没有明确边界。
4. 文件模式的普通日志必须排除 ERROR，避免与错误文件重复落盘。
5. 项目已经有 `sysOperLog` 实体和服务，但还没有和切面形成统一落库闭环。
6. 轻量化日志规则未固化，参数脱敏和大字段处理口径还不统一。

## 4. 总体设计

### 4.1 统一日志结构

三个启动模块的两种日志模式保留同一格式、级别和 `requestId`：

| 日志类别 | 用途 | 生产方式 |
| --- | --- | --- |
| 普通运行日志 | 运行与业务诊断 | `@Slf4j` / `Logger` |
| 异常和错误日志 | 故障排查 | 全局异常处理、切面异常日志 |
| 控制器访问审计日志 | 访问审计摘要 | `sys-access` Logger |

文件模式的 `root` 引用控制台及普通／错误文件 Appender，普通文件排除 ERROR，错误文件只收 ERROR；`sys-access` 引用控制台和独立访问文件，设置 `additivity=false`，每个输出目标各接收一次。保留 128MB 分卷、普通／访问 30 天、错误 60 天和各 10GB 上限的原滚动策略。

控制台模式的 `root` 和 `sys-access` 只引用控制台，访问摘要不重复输出，也不初始化文件 Appender 或创建日志目录。生产 systemd service 沿用现有标准输出／标准错误处理及日志保留配置。

### 4.2 复用现有业务模块

不重新设计日志表和日志服务，直接复用现有 `sysOperLog` 模块完成审计落库。

当前可直接复用的对象：

- `sysOperLog`
- `sysOperLogCreateDto`
- `IsysOperLogService`

其中 `IsysOperLogService` 继承了 `IService<sysOperLog>`，切面构造出 `sysOperLog` 后可直接调用 `save(...)` 完成持久化。

### 4.3 轻量化日志原则

- 默认只记录摘要，不记录完整大对象
- 默认不记录完整响应体
- 默认不记录文件流、二进制流、请求/响应原始对象
- 审计日志落库只保存必要字段
- 同一类错误不重复输出完整堆栈

## 5. 职责划分

这是本次设计的核心。

### 5.1 `@Around` 职责

`@Around` 负责完整的日志采集和落库主流程，只做“采集”和“记录”，不负责请求生命周期清理。

`@Around` 必须负责以下事项：

- 读取注解
- 获取请求参数
- 执行业务方法
- 获取返回结果
- 捕获异常
- 计算耗时
- 构造 `sysOperLog`
- 落库

推荐执行顺序：

1. 读取方法上的 `@Log`
2. 获取 `HttpServletRequest`
3. 获取类名、方法名、请求 URL、请求方式、IP
4. 从 `LoginUserInfoModelContext` 读取当前登录用户
5. 使用 `LogUtils` 处理请求参数
6. 执行 `proceed()`
7. 获取返回结果并按开关决定是否记录
8. 捕获异常并生成失败日志
9. 计算耗时
10. 构造 `sysOperLog`
11. 按日志配置输出 `sys-access` 摘要：文件模式写入 `vita-<app>-access.log` 并输出控制台，控制台模式只输出控制台
12. 调用 `IsysOperLogService.save(sysOperLog)` 完成落库

设计要求：

- `@Around` 使用 `try/catch/finally` 组织主流程
- `finally` 中不承担公共上下文清理职责，只允许做切面局部兜底
- `@Around` 生成的状态值必须与现有实体保持一致：
  - `1` 成功
  - `0` 失败

### 5.2 `@After` 或过滤器/拦截器职责

清理动作与日志采集分离，避免切面既负责记录又负责请求回收。

`@After` 或过滤器/拦截器负责以下事项：

- 清理 `LoginUserInfoModelContext`
- 清理 `MDC`
- 清理 `ThreadLocal`

推荐边界如下：

- 过滤器或拦截器的 `afterCompletion`：
  - 清理 `LoginUserInfoModelContext`
  - 清理 MDC 中的 `requestId`
  - 清理请求级 ThreadLocal
- `LogAspect` 中的 `@After`：
  - 仅清理切面内部使用的 `TIME_THREADLOCAL`

这样处理的原因：

- 过滤器/拦截器对所有请求都生效，适合负责请求级清理
- `@After` 只对切点方法生效，更适合清理切面私有状态
- 即使某个接口没有命中 `@Log`，过滤器/拦截器仍能保证上下文被清空

## 6. 推荐执行链路

建议将日志链路拆成三层：

### 6.1 过滤器/拦截器前置处理

负责请求进入时的上下文准备：

- 生成或透传 `requestId`
- 写入 `MDC.put("requestId", requestId)`
- 按需把 Sa-Token 会话中的用户信息放入 `LoginUserInfoModelContext`

### 6.2 `@Around` 审计采集

负责控制器方法的日志采集、异常感知、耗时统计和 `sysOperLog` 构造落库。

### 6.3 过滤器/拦截器后置清理

在请求完成后统一执行：

- `LoginUserInfoModelContext.removeLoginUserInfo()`
- `MDC.remove("requestId")`
- 清理其他请求级 `ThreadLocal`

如果切面内部仍保留 `TIME_THREADLOCAL`，则在 `@After` 中执行 `remove()` 兜底。

## 7. `sysOperLog` 构造规范

本次不调整表结构，直接按照当前实体字段落库。

当前 `sysOperLog` 字段包括：

- `module`
- `businessType`
- `requestMethod`
- `operUrl`
- `operIp`
- `operLocation`
- `operName`
- `operUserId`
- `className`
- `methodName`
- `requestParam`
- `responseResult`
- `status`
- `errorMsg`
- `costTime`
- `operTime`

推荐字段映射：

| `sysOperLog` 字段 | 来源 |
| --- | --- |
| `module` | `@Log.module()` |
| `businessType` | `@Log.businessType()` |
| `requestMethod` | `request.getMethod()` |
| `operUrl` | `request.getRequestURL()` |
| `operIp` | `request.getRemoteAddr()` 或统一 IP 工具 |
| `operLocation` | 可选填充，没有能力时置空 |
| `operName` | `LoginUserInfoModel.username` |
| `operUserId` | `LoginUserInfoModel.id` |
| `className` | 目标类全名 |
| `methodName` | 目标方法名 |
| `requestParam` | `LogUtils.toRequestLog(...)` |
| `responseResult` | `LogUtils.toResponseLog(...)` |
| `status` | 成功 `1`，失败 `0` |
| `errorMsg` | 异常摘要 |
| `costTime` | 结束时间 - 开始时间 |
| `operTime` | 当前时间 |

## 8. `LogUtils` 设计

本次日志治理统一收敛到一个工具类：`LogUtils`。

### 8.1 职责

`LogUtils` 统一负责：

- 敏感字段脱敏
- 大字段截断
- 文件与流对象过滤
- 请求参数摘要序列化
- 返回结果摘要序列化
- 异常摘要提取

### 8.2 建议暴露的方法

```java
String LogUtils.toRequestLog(Object[] args, String[] excludeFields);
String LogUtils.toResponseLog(Object result);
String LogUtils.toErrorLog(Throwable throwable);
boolean LogUtils.isFilterObject(Object value);
```

### 8.3 轻量化规则

- 单个字符串超长时截断
- JSON 超长时截断
- `MultipartFile`、`File`、`HttpServletRequest`、`HttpServletResponse`、`InputStream`、`OutputStream` 不写入日志
- `password`、`oldPassword`、`newPassword`、`token`、`authorization` 等字段统一脱敏
- 默认 `saveResponseData=false`

## 9. `LogAspect` 实现建议

### 9.1 切点范围

沿用当前控制器切点思路：

```java
execution(public * com.vita..controller..*Controller.*(..))
```

建议最终以“控制器切点 + 注解存在”双条件生效，避免所有控制器方法都强制写审计日志。

### 9.2 推荐骨架

`LogAspect` 推荐保留以下职责：

- 读取 `@Log`
- 调用 `LogUtils`
- 调用 `IsysOperLogService.save(sysOperLog)`
- 使用 `LoggerFactory.getLogger("sys-access")` 输出审计日志摘要

建议结构：

```java
@Around("controllerPointcut() && @annotation(log)")
public Object doAround(ProceedingJoinPoint joinPoint, Log log) throws Throwable
```

同时增加一个 `@After`：

```java
@After("controllerPointcut()")
public void clearThreadLocal()
```

这个 `@After` 只负责清理切面私有 `ThreadLocal`。

## 10. 过滤器/拦截器实现建议

### 10.1 推荐方式

优先使用过滤器或拦截器统一处理请求级上下文：

- 过滤器：适合在请求入口和出口统一处理 MDC
- `HandlerInterceptor.afterCompletion`：适合在 MVC 请求完成后统一清理

### 10.2 推荐职责

前置：

- 写入 `requestId`
- 设置 MDC
- 设置 `LoginUserInfoModelContext`

后置：

- 清理 `LoginUserInfoModelContext`
- 清理 MDC
- 清理请求级 ThreadLocal

### 10.3 推荐结论

对于当前项目，更建议：

- 过滤器或拦截器负责请求级上下文初始化和清理
- `@Around` 负责日志采集和落库
- `@After` 负责切面内部 ThreadLocal 兜底清理

## 11. 对现有日志配置的补充优化

结合三个启动模块当前的 `logback-spring.xml`，建议补充以下优化：

1. 将项目 logger 从 `com.xxx.project` 调整为 `com.vita`
2. 文件模式按普通／错误分类输出，控制台模式不初始化文件 Appender
3. 审计日志统一写入 `sys-access` logger
4. MDC 键统一为 `requestId`，不要再使用 `LOG_ID`
5. 补齐 `spring-boot-starter-aop` 依赖，确保切面能力完整生效
6. 文件模式沿用异步 Appender 和滚动器；生产选择控制台模式，由 systemd 接管日志
7. 非核心日志异步统一走默认线程池，不再拆分操作日志、登录审计、地理位置补全的独立线程池
8. 登录地点查询只允许走缓存或异步补全，不在请求线程内同步调用外部 IP 服务

## 12. 开发落地顺序

1. 统一 `requestId` 生成、透传和清理逻辑
2. 完善 `LogUtils`
3. 完善 `@Log` 注解字段定义
4. 完成 `LogAspect @Around`
5. 增加 `LogAspect @After` 清理切面私有 `ThreadLocal`
6. 增加过滤器或拦截器，统一清理 `LoginUserInfoModelContext`、MDC 和请求级 ThreadLocal
7. 对接现有 `IsysOperLogService` 完成审计日志落库
8. 验证三个模块的文件模式产生三类日志，审计摘要每个目标只输出一次
9. 验证 prod 默认控制台模式不创建日志文件，本地 prod 手动切换文件模式有效

## 13. 验收标准

- 控制器命中 `@Log` 时，`@Around` 能完整执行注解读取、参数获取、方法执行、结果获取、异常捕获、耗时统计、`sysOperLog` 构造与落库
- 请求结束后，`LoginUserInfoModelContext`、MDC、ThreadLocal 都能被清理
- `requestId` 在业务日志和审计日志中可串联
- `sysOperLog` 直接复用现有实体和服务，不引入新表结构
- 审计日志默认轻量化输出，不记录完整大对象和敏感信息
- 控制台访问审计日志与数据库审计记录内容口径一致，均为摘要信息
- 登录主链路不再等待外部 IP 归属地 HTTP 请求
- 非核心日志异步统一走默认线程池
- 控制台模式不创建日志文件或日志目录，文件模式按原策略滚动日志
- 控制台审计日志每条输出一次

## 14. YAML 日志模式与生产部署

三个启动模块的 `application-dev.yml` 默认：

```yaml
logging:
  config: classpath:logback-spring.xml
```

`application-prod.yml` 默认：

```yaml
logging:
  config: classpath:logback-console.xml
```

- 本地使用 prod 且需要保存文件时，将本机 `application-prod.yml` 中的 `logging.config` 改为 `classpath:logback-spring.xml`；切回控制台模式则改为 `classpath:logback-console.xml`。同一配置值在本地和生产行为一致。
- profile YAML 保持 Git 忽略，上述示例是可跟踪的配置说明，不包含私有值。
- 生产通过 systemd service 管理 JAR，显式启用 prod，确认实际加载的 prod YAML 选择控制台模式，日志位置和轮转由 service 现有配置接管。
- 若启动命令已显式指定 `--logging.config`，该参数会覆盖 YAML；采用 YAML 切换时应移除冲突的启动参数。
- 不增加环境变量占位符、布尔开关、Java 初始化逻辑或条件处理依赖。重新构建并重启应用后生效；本次不修改或重启生产 service。
