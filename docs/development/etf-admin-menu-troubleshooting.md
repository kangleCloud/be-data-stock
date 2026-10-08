# ETF 管理菜单核查与升级

接口和数据字段见 [三大屏 V1 契约](market-platform-v1-contract.md)。管理页面由登录后的 `GET /admin/api/auth/routers` 返回的数据库菜单生成，编译通过不会自动执行 SQL 或刷新浏览器已加载的动态路由。

## 已确认与未确认

- 四个 Vue 页面文件均存在，SQL `component_path` 与前端动态组件解析规则一致。
- `SysMenuMapper.selectAllEnabledMenus` 查询 `status=1 AND is_deleted=0`；超级管理员以用户 `is_super_admin` 标记直接查询全部启用菜单，不依赖 `SUPER_ADMIN` 角色关联。普通账号需要 `sys_role_menu`，接口操作另外需要 `sys_role_permission`。
- 路由的 `hidden` 来自 `visible=0`；权限码没有额外过滤 ETF 页面。
- 2026-10-03 本机 19001 有进程监听；获准访问本机网络后，`/admin/api/auth/routers` 返回 HTTP 200、业务码 401“账号未登录”。没有取得登录用户的菜单响应。
- 使用本机 `application-dev.yml` 的数据库配置执行只读连接失败，SQLState 为 `08S01`。未核实实际菜单、角色关联或迁移执行状态，也未证明该配置与运行中服务的数据源相同。

## SQL 修复

初始化菜单位于 `sql/init/insert.sql`，现有环境升级使用 `sql/upgrade/20261002_etf_monitor_v1.sql`。两处菜单和权限种子一致。

升级脚本原先固定父菜单 ID 为 1000，只在 `route_name` 不存在时插入。已调整为查找未删除的 `/system`、类型 `CONTENTS` 的实际父菜单，并在重复执行时修正四个页面的父节点、路由名、路径、组件和权限关联。保留已有菜单的启停、可见性及权限启停设置。升级前必须确认系统根目录存在；如已存在重复路由，先人工核查，脚本不删除菜单。

| 页面 | route_name | route_link | component_path | 查看权限 |
| --- | --- | --- | --- | --- |
| ETF监控 | EtfMonitor | /system/etfMonitor | system/etfMonitor/index | system:etf-monitor:view |
| ETF字典 | EtfDictionary | /system/etfDictionary | system/etfDictionary/index | system:etf-dictionary:view |
| ETF资料 | EtfProfile | /system/etfProfile | system/etfProfile/index | system:etf-profile:view |
| 核心指数配置 | MarketIndexConfig | /system/indexConfig | system/indexConfig/index | system:index-config:view |

修改 ETF 清单另需 `system:etf-monitor:update`，同步资料需 `system:etf-monitor:refresh`，修改指数配置需 `system:index-config:update`。脚本幂等补齐 `SUPER_ADMIN` 角色的系统父菜单、四个页面及七项权限关联；普通角色按实际职责授权。

## 部署后检查步骤

1. 在目标数据库只读检查 `/system` 根目录、四项菜单的 `status=1/visible=1/is_deleted=0`、路由字段及角色关联。
2. 经部署授权执行上述升级文件；本次诊断没有执行数据库写入或迁移。
3. 核对前端部署产物包含四个页面。注销后重新登录并完整刷新浏览器，使 Session 权限缓存和前端动态路由重新加载。
4. 检查登录后的 `/auth/routers` 返回四个页面；普通账号同时检查 `/auth/info` 的权限码。接口未返回菜单时查数据库与角色授权；接口已返回而侧栏未显示时查 `hidden`、前端路由状态和组件加载错误。
