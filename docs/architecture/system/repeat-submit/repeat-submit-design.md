# 重复提交防护注解设计

## 目标

为业务控制器提供手动标注的短时间重复提交防护，命中后直接返回 `GlobalErrorCode.REPEATED_REQUESTS`。

## 作用范围

- 仅作用于 `com.vita..controller..*Controller` 下标注 `@RepeatSubmit` 的方法
- 默认覆盖 `vita-admin`、`vita-openapi`、`vita-scheduler`、`vita-workflow`
- Starter 内的 Controller 只有在应用实际装配为 Bean 且方法显式标注 `@RepeatSubmit` 时才生效

## 用法

```java
@RepeatSubmit(interval = 5, unit = TimeUnit.SECONDS)
@PostMapping("/add")
public CommonResult<Long> create(@RequestBody @Valid XxxCreateDto dto) {
    ...
}
```

推荐仅标注写操作：

- add / update / delete
- auth / cancelAuth / grant / unbind
- refresh / trigger / submit

查询接口默认不标注。

用户名密码登录接口也不使用该固定时间窗。验证码本身一次性使用，认证失败后应允许用户立即获取新验证码并重试；登录防刷由验证码和认证风控负责，前端在请求进行中阻止并发提交。

## 判重规则

Redis key：

`repeat_submit:{HTTP_METHOD}:{REQUEST_URI}:{IDENTITY}:{ARGS_DIGEST}`

其中：

- `IDENTITY` 优先取当前登录用户 ID
- 未登录时回退客户端 IP
- `ARGS_DIGEST` 为方法参数的稳定 SHA-256 摘要

## 已复用的基础能力

- `com.vita.auth.context.LoginUserInfoModelContext`
- `com.vita.redis.RedisCache#setIfAbsent(...)`
- `com.vita.web.ip.IpUtils`
- `com.vita.core.exception.GlobalErrorCode.REPEATED_REQUESTS`

## 验证

建议执行：

```bash
mvn -pl vita-common -am test
mvn -pl vita-admin -am test
mvn -pl vita-scheduler -am test
```

如需快速检查编译，可先执行：

```bash
mvn -pl vita-common -am compile
```
