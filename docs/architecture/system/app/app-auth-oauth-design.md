# App用户认证与OAuth登录设计

## 1. 模块边界

`vita-app` 是独立启动模块，依赖方向固定为 `vita-app -> vita-service -> vita-common`。

- `vita-service/com.vita.app.user` 维护App用户、用户名规则和密码认证逻辑。
- `vita-service/com.vita.app.auth` 维护认证请求、响应和登录编排。
- `vita-service/com.vita.app.oauth` 维护OAuth绑定、临时状态、票据编排、平台适配接口和GitHub HTTP适配器。
- `vita-app` 只维护Controller、启动类和运行配置，避免业务逻辑分散在启动模块。
- `vita-admin` 不提供App用户或OAuth绑定的通用CRUD，密码密文和绑定关系不能通过管理接口直接修改。

App账号域使用 Sa-Token `loginType=app`。公共拦截器按当前应用注册的 `StpLogic` 校验登录态，
`StpInterfaceImpl` 对非默认管理账号域始终返回空角色和空权限，防止相同数字ID误命中 `sys_user` RBAC。

## 2. 数据与密码

权威DDL为 `sql/init/app.sql`，已有环境使用
`sql/upgrade/20260825_vita_app_auth_upgrade.sql`。表结构包括：

- `app_user`：本地用户名、SM4密码密文、公开资料和最近登录快照。
- `app_oauth_account`：平台稳定用户ID和公开资料快照，不保存平台令牌、邮箱或原始响应。

用户名写入前执行 `trim + lowercase`，仅允许4到32位字母、数字和下划线。用户名全局唯一且逻辑删除后不回收。
App密码按本项目当前统一要求复用管理端 `Sm4Utils.encryptToBase64`；SM4属于可逆加密，后续如升级为不可逆摘要必须提供独立迁移与兼容登录方案。

## 3. 接口

所有接口使用 `/app/api` 上下文：

| 方法 | 路由 | 访问规则 |
| --- | --- | --- |
| `POST` | `/auth/register` | 匿名，必须验证码，注册后不自动登录 |
| `POST` | `/auth/login` | 匿名，用户名、密码和验证码登录 |
| `POST` | `/auth/logout` | App登录态 |
| `GET` | `/auth/me` | App登录态 |
| `GET` | `/auth/oauth/{providerCode}/authorize` | 匿名，生成OAuth登录授权地址 |
| `GET` | `/auth/oauth/{providerCode}/bind-authorize` | App登录态，生成绑定授权地址 |
| `GET` | `/auth/oauth/{providerCode}/callback` | 平台回调，跳转固定前端地址 |
| `POST` | `/auth/oauth/ticket/exchange` | 匿名，消费一次性票据 |

OAuth登录只接受已经绑定的本地用户，不根据平台昵称、公开资料或邮箱自动注册、合并账号。
V1不提供解绑接口。

## 4. OAuth安全流程

平台由 `AppOAuthProvider` 扩展，OAuth Service只允许调用已注册且启用的实现。GitHub是首个适配器，使用官方Web Application Flow和PKCE S256，
不申请 `user:email` scope。GitHub当前授权码流程及PKCE参数以
[GitHub OAuth官方文档](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)为准。

1. 后端生成随机 `state`、`code_verifier` 和 `code_challenge`，state默认在Redis保存5分钟。
2. GitHub回调通过Redis原子GETDEL消费state，再使用授权码和verifier换取临时access token。
3. access token只在适配器方法局部变量中使用，查询公开用户资料后丢弃。
4. 后端完成登录身份确认或绑定后，生成默认60秒有效的一次性ticket并跳转到固定前端地址。
5. 前端调用票据兑换接口；Redis再次使用GETDEL保证票据只能消费一次。只有LOGIN用途签发App Sa-Token，BIND用途只返回绑定结果。

回调URL只携带随机ticket或固定错误码，不携带GitHub token、App token、用户ID、授权码或原始错误描述。

## 5. 配置与扩展

核心配置前缀：

- `vita.app.oauth.frontend-redirect-uri`
- `vita.app.oauth.state-timeout`
- `vita.app.oauth.ticket-timeout`
- `vita.app.oauth.github.enabled`
- `vita.app.oauth.github.client-id`
- `vita.app.oauth.github.client-secret`
- `vita.app.oauth.github.callback-uri`

`vita-admin`、`vita-openapi`、`vita-scheduler`、`vita-app` 均沿用仓库现有配置文件规则，
数据库、Redis、JWT及OAuth配置直接维护在对应的 `application.yml`、`application-*.yml` 中，不强制使用环境变量。
GitHub OAuth默认关闭，启用前需在目标环境配置中补齐客户端ID、客户端密钥和回调地址。
新增OAuth平台时实现 `AppOAuthProvider` 并声明新的平台属性，不修改用户表、绑定表、Controller路由或核心OAuth编排。
