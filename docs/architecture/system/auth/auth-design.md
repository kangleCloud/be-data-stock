# 管理端用户认证开发文档

> 本文基于现有《管理后台用户登录》设计稿进行完善与优化，补充了认证边界、核心流程、数据模型建议、Redis 键设计、接口设计、异常处理、安全建议与落地实现说明，适用于 Spring Boot + Sa-Token/JWT + Redis + MySQL 的管理端认证场景。

---

## 1. 文档目标

用于指导管理端用户认证模块的设计、开发、联调与后续扩展，覆盖以下内容：

- 登录认证流程
- 验证码校验机制
- JWT / Sa-Token 登录态设计
- 用户、登录日志、IP 封禁数据结构
- Redis 缓存与登录风控设计
- 安全控制与异常处理建议
- 接口返回与开发实现建议

---

## 2. 适用范围

适用于管理后台 Web 端登录认证场景，包括：

- 用户名 + 密码登录
- 图形验证码校验/滑块验证校验
- 登录失败次数控制
- IP 临时/永久封禁
- 登录态签发与注销
- 登录日志记录
- 后续 OAuth / 单点登录扩展

### 管理端联调约定

- 管理端默认开启 `char` 字符图片验证码，登录页通过 `GET /captcha/captcha` 获取图片、UUID 与加密数据。
- 用户列表和详情禁止返回密码；用户更新未传密码时保持原值，传入新密码时由服务端完成 SM4 加密并更新密码修改时间。
- 系统内置用户和超级管理员用户不允许通过用户管理接口删除。

---

## 3. 设计目标

### 3.1 安全性

- 防止暴力破解、撞库、恶意刷接口
- 防止未授权访问管理后台资源
- 对登录行为进行审计留痕
- 支持用户状态禁用、IP 封禁、验证码防刷

### 3.2 可维护性

- 认证流程清晰，便于模块拆分与后续扩展
- 用户认证、风控、日志、会话管理职责分离
- Redis Key、数据库表、接口结构命名统一

### 3.3 可扩展性

- 支持后续接入MFA 二次认证
- 支持多租户隔离
- 支持登录设备管理、强制下线、token 刷新等能力

### 3.4 性能与体验

- 登录主链路尽量短，非关键操作异步化
- 验证码、封禁状态优先走 Redis
- 登录成功后返回基础用户信息与权限初始化数据

---

## 4. 认证架构说明

推荐将认证模块拆分为以下职责层：

### 4.1 认证核心服务

负责用户名密码校验、验证码校验、状态校验、登录态签发。

### 4.2 风控服务

负责登录失败次数统计、IP 封禁、账号锁定、限流等。

### 4.3 会话服务

负责 token、session、在线用户信息、登出、强制下线等。

`LoginUserInfoModel` 已迁移至 `com.vita.auth.model`。由于旧登录会话可能保存原全限定类名，发布该结构调整时必须清理 Sa-Token 会话命名空间，并要求在线用户重新登录；系统不保留旧包兼容反序列化逻辑。

### 4.4 审计日志服务

负责记录登录成功/失败日志、来源 IP、浏览器、操作系统、登录地点等。
登录地点优先使用本地缓存命中结果；缓存未命中时先落 `XX XX` 占位，再通过异步任务补全 `sys_login_log.login_location`。

---

## 5. 整体认证流程

```text
前端提交用户名/密码/验证码
        ↓
校验基础参数
        ↓
校验验证码（如果启用）
        ↓
校验 IP 是否已被封禁
        ↓
查询用户信息
        ↓
校验用户状态（是否删除/禁用）
        ↓
校验密码
        ↓
登录失败处理（失败计数、日志记录、必要时封禁）
        ↓
登录成功
        ↓
签发 token / 建立 Sa-Token 会话
        ↓
写入 session 用户信息
        ↓
异步更新登录时间/IP
        ↓
异步记录登录成功日志
        ↓
异步补全登录地点
        ↓
返回 token + 用户基础信息
```

---

## 6. 核心业务流程设计

## 6.1 登录前置校验

登录接口在进入用户认证前，应先完成以下校验：

1. 用户名不能为空
2. 密码不能为空
3. 用户名长度必须在系统限定范围内
4. 密码长度必须在系统限定范围内
5. 请求来源 IP 是否被封禁
6. 验证码功能开启时，验证码参数不能为空

建议：

- 用户名长度：`4 ~ 32`
- 密码长度：`8 ~ 64`
- 对外统一返回通用错误提示，避免暴露过多账户信息

---

## 6.2 验证码校验

当系统启用验证码后，登录请求需先完成验证码校验。

### 验证步骤

1. 根据验证码唯一标识从 Redis 读取验证码值
2. 判断验证码是否存在、是否过期
3. 比对用户输入是否一致
4. 校验完成后立即删除 Redis 中的验证码，防止重复使用

### Redis Key 建议

```text
auth:captcha:{uuid}
```

### 建议规则

- 验证码有效期：`2~5 分钟`
- 验证码一次性使用
- 连续失败达到阈值后强制启用验证码

---

## 6.3 IP 封禁校验

登录前应优先检查当前请求 IP 是否处于封禁状态。

### 检查顺序

1. 系统启动时将数据库中生效的 IP 封禁记录加载到 Redis,并设置过期时间与封禁结束时间一致
2. 先查 Redis：`auth:ip:block:{ip}`

### Redis Key 建议

```text
auth:ip:block:{ip}
```

### 建议策略

- 临时封禁：例如连续失败 5 次，封禁 30 分钟
- 永久封禁：由管理员手动配置
- Redis TTL 与封禁结束时间一致
- 永久封禁 Redis 可设置较长 TTL，并定时与数据库同步

---

## 6.4 用户身份校验

### 用户查询

根据用户名查询 `sys_user`，建议查询条件：

- `user_name = ?`
- `is_deleted = 0`
- 多租户场景下追加 `tenant_id = ?`

### 用户状态校验

查询到用户后，应校验：

- 是否已被逻辑删除
- 是否已被禁用 `status = 0`
- 是否需要校验密码更新时间、账号有效期等扩展属性

### 失败场景

以下任一情况均应终止登录：

- 用户不存在
- 用户被禁用
- 用户已删除
- 租户不存在或租户已禁用

> 建议前端统一提示“用户名或密码错误”或“账号已被禁用”，避免对不存在用户返回过于明确的信息。

---

## 6.5 密码校验

当前设计稿提到“通过 SM4 算法加密用户输入密码后比对数据库”。

---

## 6.6 登录失败处理

登录失败时，不应只返回错误，还应触发风控处理。

### 处理项

1. 记录失败日志
2. 记录失败次数到 Redis
3. 达到阈值时自动封禁 IP 或触发验证码
4. 必要时触发账号锁定策略

### Redis Key 建议

```text
auth:login:fail:ip:{ip}
auth:login:fail:user:{userName}
```

### 建议策略

- 单 IP 连续失败 5 次：封禁 30 分钟

- 单账号连续失败 5 次：要求验证码



### 注意事项

- 失败计数建议设置过期时间，如 `30 分钟`
- 登录成功后清理该账号/IP 失败计数

---

## 6.7 登录成功处理

用户认证通过后，执行登录成功链路：

1. 生成登录态
2. 将用户基础信息写入 session
3. 返回 token 与用户基本资料
4. 异步更新用户登录信息
5. 异步记录登录成功日志
6. 缓存未命中时异步补全登录地点

说明：

- 登录相关异步任务统一走系统默认异步线程池，不再单独拆分登录审计线程池
7. 清理登录失败计数

### 推荐登录态方案

基于 Sa-Token：

```java
StpUtil.login(userId);
StpUtil.getSession().set(Constants.LOGIN_USER_INFO, loginUserInfo);
```

如需返回自定义 token：

- 可使用 Sa-Token 的 token 值直接返回
- 或统一封装 JWT/自定义扩展信息，但应避免双 token 体系复杂化

### 建议返回内容

- token
- tokenName
- 过期时间
- 用户 ID
- 用户名
- 昵称
- 头像
- 是否超级管理员
- 角色列表/权限标识（可按需延迟加载）

---

## 6.8 登出流程

登出时应执行：

1. 解析当前登录态
2. 清理 session
3. 使 token 失效
4. 记录登出日志（可选）

Sa-Token 示例：

```java
StpUtil.logout();
```

---

## 7. 数据库表设计

以下表结构沿用原设计，并给出优化建议。

## 7.1 用户表 `sys_user`

`sys_user` 作为业务主表样例，字段布局统一遵循 `id -> tenant_id -> 业务字段 -> 公共审计后缀`。其中 `pwd_update_date` 属于用户域业务字段，不属于全局强制公共字段。

```sql
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              bigint                                                        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`       bigint                                                                 DEFAULT NULL COMMENT '租户ID',
    `dept_id`         bigint                                                                 DEFAULT NULL COMMENT '部门ID',
    `nick_name`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '昵称',
    `user_name`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '用户名称',
    `password`        varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
    `mobile`          varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '手机号',
    `email`           varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '邮箱',
    `avatar_url`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '头像',
    `gender`          char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci               DEFAULT '2' COMMENT '性别(0-女,1-男,2-其他)',
    `status`          tinyint                                                       NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
    `is_super_admin`  tinyint                                                       NOT NULL DEFAULT '0' COMMENT '是否超级管理员 1是 0否',
    `is_system`       tinyint                                                       NOT NULL DEFAULT '0' COMMENT '是否系统内置 1是 0否',
    `login_time`      datetime                                                               DEFAULT NULL COMMENT '上次登录时间',
    `login_address`   varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '登录地址',
    `pwd_update_date` datetime                                                               DEFAULT NULL COMMENT '密码最后更新时间',
    `create_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`     datetime                                                               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1)                                                              DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`    bigint                                                                 DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`    bigint                                                                 DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                                                                 DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name` (`user_name`),
    KEY `idx_user_user_name` (`user_name`),
    KEY `idx_user_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息表';
```

### 优化建议

1. `password` 字段长度保留 200 足够兼容 BCrypt/Argon2
2. 建议增加联合索引：`(tenant_id, user_name, is_deleted)`
3. 可增加字段：
   - `login_ip`：最近登录 IP
   - `login_browser`
   - `login_os`
   - `pwd_error_count`（如失败次数需要落库）
   - `account_lock_end_time`（账号临时锁定）
4. 若系统要求手机号唯一、邮箱唯一，可增加唯一索引

---

## 7.2 登录日志表 `sys_login_log`

```sql
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`
(
    `id`             bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
    `user_name`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '用户账号',
    `ipaddr`         varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录IP地址',
    `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录地点',
    `browser`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '浏览器类型',
    `os`             varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '操作系统',
    `status`         char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci      DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
    `msg`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '提示消息',
    `login_time`     datetime                                                      DEFAULT NULL COMMENT '访问时间',
    `create_time`    datetime                                                      DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`    datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint(1) DEFAULT '0' COMMENT '是否删除',
    `tenant_id`      bigint                                                        DEFAULT NULL COMMENT '租户ID',
    `create_by_id`   bigint                                                        DEFAULT NULL COMMENT '新增用户ID',
    `create_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`   bigint                                                        DEFAULT NULL COMMENT '更新用户ID',
    `update_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新用户名称',
    `version`        bigint                                                        DEFAULT '0' COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    KEY `idx_sys_logininfor_s` (`status`),
    KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统访问记录';
```

### 优化建议

1. 建议增加 `user_id` 字段，便于后续关联用户查询
2. 建议增加索引：
   - `(user_name, login_time)`
   - `(ipaddr, login_time)`
3. 建议 `status` 改为 tinyint，更利于语义统一
4. 登录失败原因 `msg` 不宜返回过细，但日志中可保留

---

## 7.3 IP 封禁表 `sys_ip_block`

```sql
DROP TABLE IF EXISTS `sys_ip_block`;
CREATE TABLE `sys_ip_block`
(
    `id`           bigint                                  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `ip`           varchar(64) COLLATE utf8mb4_general_ci  NOT NULL COMMENT 'IP地址',
    `status`       tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
    `tenant_id`    bigint                                           DEFAULT NULL COMMENT '租户ID',
    `remark`       varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    `create_by_id` bigint                                           DEFAULT NULL COMMENT '创建人ID',
    `create_by`    varchar(64) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '创建人',
    `create_time`  datetime                                         DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by_id` bigint                                           DEFAULT NULL COMMENT '更新人ID',
    `update_by`    varchar(64) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '更新人',
    `update_time`  datetime                                         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`      int                                              DEFAULT '0' COMMENT '版本号',
    `is_deleted`   tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_ip_status` (`ip`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IP封禁表';
```

### 优化建议

1. `status` 语义统一为：`1-启用，0-禁用`，避免业务状态含义分裂
2. 建议增加字段：
   - `block_type`：`manual / auto`
   - `reason`
   - `source`：来源模块
3. 建议增加索引：`(ip, status, end_time)`
4. 可增加定时任务，自动失效已过期封禁记录

---

## 8. Redis Key 设计建议

```text
auth:captcha:{uuid}                 -> 验证码

auth:ip:block:{ip}                 -> IP封禁标记

auth:login:fail:ip:{ip}            -> IP登录失败次数

auth:login:fail:user:{userName}    -> 用户登录失败次数

```

### 设计建议

- 统一前缀：`auth:`
- Key 命名语义清晰，可直接定位业务含义
- 明确 TTL，避免脏数据长期残留

---

## 9. 接口设计建议

## 9.1 登录接口

### 请求地址

```http
POST /api/auth/login
```

### 请求示例

```json
{
  "userName": "admin",
  "password": "123456",
  "captcha": "ABCD",
  "captchaKey": "9c6e4d0d-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

### 返回示例

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "tokenName": "Authorization",
    "tokenValue": "xxxxx.yyyyy.zzzzz",
    "expiresIn": 7200,
    "userInfo": {
      "userId": 1,
      "userName": "admin",
      "nickName": "系统管理员",
      "avatarUrl": "",
      "isSuperAdmin": 1,
      "isSystem": 0
    }
  }
}
```

---

## 9.2 获取当前登录用户信息

```http
GET /api/auth/info
```

返回当前用户基础信息、角色、菜单权限等。

---

## 9.3 退出登录

```http
POST /api/auth/logout
```

---

## 10. 异常码与提示建议

| 场景 | 建议提示 |
|---|---|
| 参数为空 | 请求参数错误 |
| 验证码错误 | 验证码错误或已过期 |
| IP 被封禁 | 当前登录环境已被限制，请稍后再试 |
| 用户不存在 | 用户名或密码错误 |
| 密码错误 | 用户名或密码错误 |
| 用户被禁用 | 账号已被禁用，请联系管理员 |
| 登录成功 | 登录成功 |
| token 失效 | 登录状态已失效，请重新登录 |

> 建议错误码统一由 `AuthErrorCode` 枚举维护，便于前后端联调与国际化扩展。

---

## 11. 安全设计建议

## 11.1 必做项

- 全站 HTTPS
- 密码哈希存储
- 登录接口限流
- 验证码防刷
- IP 黑名单/临时封禁
- 登录日志留痕
- token 失效控制
- 前后端统一异常处理

## 11.2 建议项

- 密码复杂度校验
- 首次登录强制改密
- 定期密码过期提醒
- 异地登录提醒
- 多端登录控制
- 超级管理员单独二次认证
- 支持 MFA（短信 / TOTP）

## 11.3 风险点

- 密码可逆加密存储存在泄露风险
- 登录失败不计数容易被暴力破解
- 日志未异步化会拖慢主链路
- 登录地点若同步调用外部 IP 服务，会直接放大登录耗时与超时风险
- Redis 与 DB 状态不同步会导致封禁不一致

---

## 12. 开发落地建议

## 12.1 模块划分建议

```text
auth
├── controller
│   └── AuthController
├── service
│   ├── AuthService
│   ├── CaptchaService
│   ├── LoginRiskService
│   ├── LoginLogService
│   └── TokenService
├── model
│   ├── req
│   ├── resp
│   └── dto
├── mapper
├── entity
└── enums/constants
```

---

## 12.2 核心方法建议

### AuthService

- `login(LoginReq req)`
- `logout()`
- `getLoginUserInfo()`

### CaptchaService

- `validate(String captchaKey, String captchaCode)`

### LoginRiskService

- `checkIpBlock(String ip)`
- `recordFailByIp(String ip)`
- `recordFailByUser(String userName)`
- `clearFailCount(String ip, String userName)`

### LoginLogService

- `recordSuccess(...)`
- `recordFail(...)`

---

## 12.3 登录伪代码示例

```java
public LoginResp login(LoginReq req) {
    // 1. 基础参数校验
    validateLoginParam(req);

    // 2. 验证码校验
    if (captchaEnabled) {
        captchaService.validate(req.getCaptchaKey(), req.getCaptcha());
    }

    String ip = IpUtils.getIpAddr();

    // 3. IP 封禁校验
    loginRiskService.checkIpBlock(ip);

    // 4. 查询用户
    SysUser user = userService.findByUserName(req.getUserName());
    if (user == null || Boolean.TRUE.equals(user.getIsDeleted())) {
        asyncRecordLoginFail(req, ip, "用户名或密码错误");
        loginRiskService.recordFailByIp(ip);
        loginRiskService.recordFailByUser(req.getUserName());
        throw new BusinessException("用户名或密码错误");
    }

    // 5. 用户状态校验
    if ("1".equals(user.getStatus())) {
        asyncRecordLoginFail(req, ip, "账号已禁用");
        throw new BusinessException("账号已被禁用，请联系管理员");
    }

    // 6. 密码校验
    if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
        asyncRecordLoginFail(req, ip, "用户名或密码错误");
        loginRiskService.recordFailByIp(ip);
        loginRiskService.recordFailByUser(req.getUserName());
        throw new BusinessException("用户名或密码错误");
    }

    // 7. 登录成功
    StpUtil.login(user.getId());
    LoginUserInfo loginUserInfo = buildLoginUserInfo(user);
    StpUtil.getSession().set(Constants.LOGIN_USER_INFO, loginUserInfo);

    // 8. 清理失败计数
    loginRiskService.clearFailCount(ip, req.getUserName());

    // 9. 异步处理
    asyncRecordLoginSuccess(user, ip);
    asyncUpdateLoginInfo(user, ip);

    // 10. 返回结果
    return buildLoginResp(user);
}
```

---

## 13. 与权限体系的关系

认证与授权建议分层处理：

- **认证**：解决“你是谁”
- **授权**：解决“你能访问什么”

### 推荐做法

登录成功后：

1. 完成身份认证
2. 在获取用户信息接口中，加载角色与菜单权限
3. 将关键权限标识缓存到 session 或 Redis
4. 接口访问通过 Sa-Token 权限注解或统一拦截器完成授权校验

例如：

```java
@SaCheckPermission("system:user:list")
@GetMapping("/list")
public Result<?> list() {
    return Result.ok();
}
```

---

## 13.1 工作流认证边界

`vita.auth.extra-exclude-paths` 在 `vita-admin/src/main/resources/application.yml` 集中维护，profile 配置和 workflow 配置不得重复声明该列表。

- 匿名访问：`/warm-flow-ui/index.html`、`/warm-flow-ui/css/**`、`/warm-flow-ui/js/**`、`/warm-flow-ui/ico/**`、`/warm-flow-ui/config`
- 登录访问：`/warm-flow/published-form`、`/warm-flow/form-content/**`、`/warm-flow/execute/load/**`、`/warm-flow/execute/hisLoad/**`、`/warm-flow/execute/handle`
- 管理员角色访问：其余 `/warm-flow/**`，仅允许 `SUB_ADMIN` 或 `SUPER_ADMIN`
- 管理访问：`/system/workflow/**` 在登录基础上继续执行权限码与管理员角色校验
- 禁止放行 `/warm-flow/**` 或 `/warm-flow-ui/**` 整体路径

Sa-Token 注解鉴权在路由登录校验前执行。`SaTokenExceptionHandler` 必须保持高于通用异常处理器的优先级，确保 `NotLoginException`、`NotPermissionException` 和 `NotRoleException` 分别返回正确的 401/403 业务码，而不是被通用异常处理器转为 500。

---

## 14. 后续扩展建议

后续可逐步扩展以下能力：

1. OAuth2 / 单点登录
2. 手机号登录
3. 邮箱验证码登录
4. 二次认证 MFA
5. 登录设备管理
6. 强制下线
7. token 刷新机制
8. 登录行为风控画像
9. 异地登录告警
10. 超级管理员专属认证链路

---

## 15. 本次优化总结

相较原始设计稿，本版重点优化了以下内容：

- 补充了认证模块整体分层与职责边界
- 细化了验证码、IP 封禁、失败计数、登录成功处理流程
- 对密码存储方案进行了安全性优化建议
- 补充了 Redis Key 设计规范
- 增加了接口设计、异常提示、安全清单与伪代码示例
- 对原有三张核心表提出了结构与索引优化建议
- 明确了认证与授权的边界，便于后续接入 RBAC

---

## 16. 原始设计依据

本文基于用户提供的《管理后台用户登录》文档进行完善与优化，原始内容包含设计原则、认证流程、`sys_user`、`sys_login_log`、`sys_ip_block` 三张表结构，以及基础登录业务流程说明。fileciteturn0file0
