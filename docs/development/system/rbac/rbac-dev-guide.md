# RBAC 开发文档

## 1. 开发目标

本开发文档用于指导 RBAC 一期实现，目标是在当前仓库结构下，以最少必要改动完成：

- 当前用户角色获取
- 当前用户权限获取
- 当前用户菜单获取
- `Sa-Token` 角色/权限回调接入
- 管理端接口权限注解落地

---

## 2. 模块改造范围

### 2.1 `vita-common`

职责：

- 放公共鉴权常量、登录态模型

建议改动：

- `AuthConstants`
    - 增加 权限和角色 Redis Key，例如：
        - `LOGIN_USER_PERMISSION`
        - `LOGIN_USER_ROLE`
        - 保存角色码、权限码
- `LoginUserInfoModel`
    - 保留基础用户信息

### 2.2 `vita-service`

职责：

- 负责 RBAC 聚合查询、认证编排和 Sa-Token 回调实现

建议改动：

- `IAuthCommonService`
    - 统一承接角色码、权限码、菜单和超管判定查询
- `AuthCommonServiceImpl`
    - 实现聚合查询逻辑
- `StpInterfaceImpl`
    - 放到 `com.vita.auth` 包下
    - 优先读 `LOGIN_USER_ROLE`、`LOGIN_USER_PERMISSION`
    - 未命中再通过 `IAuthCommonService` 回源
- 相关 mapper 只保留单表查询与单表投影能力：
    - `SysUserRoleMapper`：按用户查角色 ID
    - `SysRolePermissionMapper`：按角色查权限 ID
    - `SysRoleMenuMapper`：按角色查菜单 ID
    - `SysPermissionMapper` / `SysMenuMapper`：按 ID 集合查详情、exists/count
- `AuthServiceImpl`
    - 增加当前用户权限初始化能力
    - 负责构建 `AuthInfoVo`

### 2.3 `vita-admin`

职责：

- 暴露接口，承接管理端鉴权注解

建议改动：

- `AuthController`
    - 扩展 `GET /auth/info`
- 业务控制器
    - 按权限码逐步增加 `@SaCheckPermission`

### 2.4 `sql`

职责：

- 确保初始化脚本与设计一致

建议检查：

- `sys_user.is_super_admin`
- `sys_permission.auth_tag`
- 关联表唯一索引和启用状态

---

## 3. 返回模型设计建议

用户和角色管理端列表、详情模型必须返回主键 `id`，供编辑、删除与后续授权使用；用户响应模型不得包含密码字段。

### 3.1 Redis 缓存对象

用途：

- 给 `StpInterfaceImpl` 用
- 给 `/auth/info` 初始化缓存用
- 保存角色码、权限码

### 3.2 权限初始化返回对象

建议新增：

```java
public class AuthInfoVo {
    private LoginUserInfoModel userInfo;
    private List<String> roleCodes;
    private List<String> permissionCodes;
}
```

用途：

- 管理端登录后的权限初始化接口
- 只返回“登录用户 + 角色码 + 权限码”
- 路由树通过独立的 `/auth/routers` 返回，避免把 RBAC 初始化和前端路由结构耦合在一起

### 3.3 菜单树返回对象

建议拆成两类，而不是复用同一个返回对象：

```java
List<TreeSelectModel>    // 下拉树
List<RouterVo>           // 前端路由树
```

建议约定：

- 下拉树给菜单管理、角色授权、权限配置等场景使用
- 路由树只给前端路由初始化使用
- 下拉树不要复用 `RouterVo`
- 路由树不要复用下拉树模型

---

## 4. 推荐服务拆分

### 4.1 `IAuthCommonService`

建议最少提供以下方法：

- `boolean isSuperAdmin(Long userId)`
- `List<String> queryRoleCodes(Long userId)`
- `List<String> queryPermissionCodes(Long userId)`
- `List<Long> queryMenuIds(Long userId)` 或 `List<SysMenu> queryMenus(Long userId)`

### 4.2 `AuthServiceImpl`

建议保持职责收口：

- `login`：认证，不做重 RBAC 查询
- `getCurrentAuthInfo` 或改造 `getCurrentUserInfo`
  - 聚合角色、权限
  - 构建 `AuthInfoVo`
  - 缓存角色码、权限码到 Session，供 `StpInterfaceImpl` 使用
- `getRouters`
  - 只负责获取当前用户路由树
  - 路由树构建逻辑收口到 `SysMenuService`

### 4.3 `ISysMenuService`

菜单能力建议拆分成三层：

- 原始查询
  - `getMenuList(searchDto)`：菜单管理页原始菜单查询
  - `getMenusByUserId(userId)`：当前用户授权菜单原始列表
- 树结构
  - `getMenuTreeByUserId(userId)`：当前用户菜单树
  - `getMenuTreeSelect(searchDto)`：菜单下拉树
- 路由结构
  - `getRoutersByUserId(userId)`：前端路由树

不要把“查询菜单”“构建树”“构建路由”混在一个 `buildMenus` 方法里。

### 4.4 `ISysRoleService`

角色模块建议单独补齐一组管理能力，供用户管理、权限管理和角色授权页面复用：

- 查询类
  - `page(searchDto)`：按 `roleName`、`roleCode`、`status` 分页查询
  - `getListByUserId(userId)`：查询某个用户已分配的角色
  - `getAll()`：查询全部角色
  - `getOptionListByUserId(userId)`：返回角色选择框列表，并带上当前用户是否已选中
- 校验类
  - `checkRoleNameUnique(role)`：角色名称唯一
  - `checkRoleCodeUnique(role)`：角色编码唯一
  - `checkRoleAllowed(roleId)`：校验角色是否允许操作
- 授权类
  - `cancelAuthUser(roleId, userId)`：取消单个授权用户
  - `cancelAuthUsers(roleId, userIds)`：批量取消授权用户
  - `authUsers(roleId, userIds)`：批量选择授权用户角色

注意：

- 角色模块不应提供“角色权限是否唯一”这种校验，正确边界是“角色编码是否唯一”
- 当前仓库的超级管理员语义只存在于 `sys_user.is_super_admin`
- 因此角色模块的“禁止操作”应落在 `is_system` 保护，而不是虚构一个“超级管理员角色不可操作”

---

## 5. 查询实现建议

### 5.1 实现方式

推荐按“单表查询下沉 mapper，多表聚合收口 service”的方式实现，且数据库 SQL 不使用 `join`。

原因：

- 角色、权限、菜单的单表过滤、排序属于持久化细节
- 用户角色、角色菜单、角色权限属于 RBAC 聚合编排，放 service 更清晰
- 菜单树、路由树属于展示结构转换，不应放在 mapper 或 auth service 中

落地规则：

- mapper 只做单表 `select ids`、`select list by ids`、`count exists`、`delete/update by ids`
- service 统一按“先查关联 ID -> 去重 -> 再查目标表”的方式组装结果
- 即使只有两张表，也不写 `join`

### 5.2 角色查询约束

- 只查启用角色
- 只查未删除关联
- 去重
- 管理端角色分页查询可按 `roleName`、`roleCode`、`status` 过滤
- 角色编码 `SUPER_ADMIN` 作为框架保留值，不应允许在角色表中创建或修改成该编码
- `SUB_ADMIN` 是物理角色，用于子管理员授权，可查看全部权限并创建工作流定义

### 5.2.1 角色管理补充约束

- 删除角色时，应同步清理 `sys_user_role`、`sys_role_menu`、`sys_role_permission`、`sys_role_dept_scope`
- 用户角色授权/取消授权由角色服务编排，具体持久化细节下沉到 `SysUserRoleService`
- 角色选择框场景建议返回：
  - 角色基础信息
  - `selected` 是否已分配给当前用户
  - `disabled` 是否禁止选择（如系统内置角色、禁用角色）

### 5.3 权限查询约束

- 只查启用权限
- 返回 `permission_code` 或 `auth_tag`
- 若用户级超级管理员标记为 1，直接返回 `["**:**:**"]`
- 子管理员通过 `SUB_ADMIN` 角色默认获得 `system:permission:view` 和 `system:workflow:import`

权限模块本身建议补齐：

- 条件分页：`permissionName`、`permissionCode`、`permissionType`、`menuId`、`status`
- `getAll()`：查询全部权限
- `getListByRoleId(roleId)`：先查 `permissionIds`，再查权限详情
- `getOptionListByRoleId(roleId)`：先查已选 `permissionIds`，再查全部权限并组装 `selected/disabled`
- `checkPermissionCodeUnique(permission)`：只校验权限编码
- `checkAuthTagUnique(permission)`：只校验鉴权标识
- `checkPermissionAllowed(permissionId)`：系统内置权限不允许删除
- `deleteBatch(ids)`：删除权限时同步清理 `sys_role_permission`

注意：

- `permissionName` 不做唯一约束
- `authTag` 未显式传值时，默认回填 `permissionCode`
- 系统内置权限只禁止改 `permissionCode` / `authTag` 和删除，不禁止一般描述字段修改

### 5.4 菜单查询约束

- 授权菜单原始查询：只查启用菜单
- 菜单下拉树：基于原始菜单树构建，不要复用路由结构
- 前端路由树：由 `visible` 映射为 `hidden`，不要在原始查询阶段把隐藏菜单直接裁掉
- 若需要按钮权限，不要从菜单表补推，而要从权限表获取
- 后续菜单权限、按钮权限、授权树扩展，应复用“原始菜单查询 + 树构建”链路，而不是复用 `RouterVo`

---

## 6. `StpInterfaceImpl` 实现建议

### 6.1 `getRoleList`

推荐逻辑：

1. `loginId` 转 `Long`
2. 从 Session 取 `LOGIN_USER_ROLE`
3. 命中则直接返回
4. 未命中则调用 `IAuthCommonService.getRoleCodesByUserId`
5. 若用户是超级管理员，返回虚拟角色码 `SUPER_ADMIN`

### 6.2 `getPermissionList`

推荐逻辑：

1. 从 Session 取 `LOGIN_USER_PERMISSION`
2. 命中则直接返回
3. 未命中则调用 `IAuthCommonService.getPermissionCodesByUserId`
4. 超级管理员返回 `["**:**:**"]`

### 6.3 注意事项

- 不要在 `StpInterfaceImpl` 中直接拼 SQL
- 回调实现只做读取缓存和兜底回源，不做复杂业务编排
- 真正的聚合逻辑收口到 `IAuthCommonService`

---

## 7. `/auth/info` 与 `/auth/routers` 接口建议

### 7.1 推荐返回结构

```json
{
  "userInfo": {
    "id": 1,
    "username": "admin",
    "nickName": "系统管理员",
    "deptId": 100,
    "isSuperAdmin": 1,
    "isSystem": 0
  },
  "roleCodes": [
    "SUPER_ADMIN"
  ],
  "permissionCodes": [
    "**:**:**"
  ]
}
```

### 7.2 推荐处理流程

`GET /auth/info`

1. 从 Session 取基础登录用户
2. 根据 `userId` 聚合角色码、权限码
3. 组装 `AuthInfoVo`
4. 写入 RBAC 上下文缓存
5. 返回前端

`GET /auth/routers`

1. 根据 `userId` 查询授权菜单原始列表
2. 构建菜单树
3. 构建 `RouterVo` 路由树
4. 返回前端

### 7.3 为什么不建议在 `/auth/login` 里做完

- 登录链路更容易被高频调用
- RBAC 聚合查询跨表较多
- 菜单树结构只在前端初始化时需要
- 下拉树和路由树的消费方不同，不应和登录认证链路耦合

---

## 8. 权限码规范建议

推荐统一格式：

```text
模块:资源:动作
```

示例：

- `system:user:view`
- `system:user:add`
- `system:user:update`
- `system:user:delete`
- `system:role:grant`

原则：

- 一个动作一个权限码
- 不要把 URL 直接当权限码
- 菜单权限和接口权限保持同一命名空间

推荐管理端统一映射：

- `system:user:view/add/update/delete`
- `system:role:view/add/update/delete/grant`
- `system:menu:view/add/update/delete`
- `system:permission:view/add/update/delete`
- `system:dept:view/add/update/delete`
- `system:ip-block:view/add/update/delete`
- `system:workflow:import`
- `system:workflow:definition:view`
- `system:workflow:definition:manage`
- `system:workflow:instance:view`
- `system:workflow:instance:manage`
- `system:workflow:task:view`
- `system:workflow:task:manage`
- `system:workflow:category:view`
- `system:workflow:category:manage`
- `system:workflow:assignee-rule:view`
- `system:workflow:assignee-rule:manage`
- `system:workflow:reimbursement:view`
- `system:workflow:reimbursement:resync`
- `system:oper-log:view/delete`
- `system:login-log:view/delete`

补充边界：

- 全部 `/system/*` 控制器都应落 `@SaCheckPermission`
- `/auth/*` 不加 `@SaCheckPermission`
- `SysOperLogController`、`SysLoginLogController` 只保留读和删，不对外暴露 `add/update`

工作流接口映射：

- `POST /system/workflow/importJson` -> `system:workflow:import`
- `GET /system/workflow/definitions/page`、`GET /system/workflow/definitions/{id}` -> `system:workflow:definition:view`
- `POST /system/workflow/definitions/{id}/publish`、`POST /system/workflow/definitions/{id}/unpublish` -> `system:workflow:definition:manage`
- `POST /system/workflow/definitions/{id}/copy`、`POST /system/workflow/definitions/{id}` -> `system:workflow:definition:manage`
- `GET /system/workflow/instances/**` -> `system:workflow:instance:view`
- `POST /system/workflow/instances/{id}/activity`、`POST /system/workflow/instances/{id}/invalidate` -> `system:workflow:instance:manage`
- `GET /system/workflow/tasks/**` -> `system:workflow:task:view`
- `POST /system/workflow/tasks/{id}/handlers` -> `system:workflow:task:manage`
- `GET /system/workflow/categories/**` -> `system:workflow:category:view`
- `POST /system/workflow/categories/**` -> `system:workflow:category:manage`
- `GET /system/workflow/assignee-rules/**` -> `system:workflow:assignee-rule:view`
- `POST /system/workflow/assignee-rules/**` -> `system:workflow:assignee-rule:manage`
- `GET /system/workflow/reimbursements/**` -> `system:workflow:reimbursement:view`
- `POST /system/workflow/reimbursements/resync` -> `system:workflow:reimbursement:resync`
已有环境通过 `sql/upgrade/vita_workflow_upgrade.sql` 按角色码和权限码幂等补齐工作流 RBAC 数据。该脚本以增量方式补齐 `workflow_category` 和默认分类，不删除已有自定义分类。

工作流补充边界：

- `/warm-flow-ui/config` 和设计器静态资源允许匿名访问，不参与 RBAC 权限码判定
- Warm-Flow 表单加载和办理接口要求登录，节点办理资格由 Warm-Flow `PermissionHandler` 判定
- 其余 `/warm-flow/**` 内置设计和管理接口只允许 `SUB_ADMIN` / `SUPER_ADMIN`
- `/workflow/**` 统一要求登录，实例查看和任务办理还必须通过 service 层归属检查
- `/system/workflow/**` 是管理端接口，必须同时具备对应 `system:workflow:*` 权限和 `SUB_ADMIN` / `SUPER_ADMIN` 角色
- 定义管理支持查看、导出、发布、取消发布、复制、激活、挂起和安全删除
- 实例管理只提供激活、挂起和作废，不提供删除、管理员强改办理人或任意变量修改

---

## 9. 测试建议

### 9.1 单元测试

至少覆盖：

- 超级管理员直接拥有全部权限
- 普通用户根据角色获取权限
- 停用角色后权限失效
- 停用权限后权限失效
- 空角色用户返回空集合
- `authTag` 默认回填 `permissionCode`
- 内置权限保护、批量删除级联清理
- 角色权限授权/取消授权
- 权限选择框、角色选择框组装

### 9.2 集成测试

至少覆盖：

- `/auth/info` 返回角色码、权限码
- `/auth/routers` 返回当前用户路由树
- 菜单下拉树接口返回 `TreeSelectModel`
- `view/add/delete/grant` 四类 `@SaCheckPermission` 落地
- 日志控制器不再暴露 `add/update`

### 9.3 回归测试

重点关注：

- 登录接口响应时间是否被明显拉长
- 未登录接口是否被误判
- 超级管理员是否被普通角色覆盖

---

## 10. 一期不做但要留口的内容

以下内容不建议在一期一起做完：

- 数据范围 SQL 自动注入
- 字段权限
- 接口路径自动映射为权限码
- Redis 级分布式权限缓存

但要提前留好扩展位置：

- `data_scope_type`
- `sys_role_dept_scope`
- `LOGIN_USER_ROLE`
- `LOGIN_USER_PERMISSION`
- `IAuthCommonService`

---

## 11. 实施完成标志

满足以下条件即可开始进入业务接口批量接入阶段：

- RBAC 聚合服务已完成
- `/auth/info` 已返回权限初始化数据
- `/auth/routers` 已返回当前用户路由树
- 菜单下拉树已独立返回 `TreeSelectModel`
- `StpInterfaceImpl` 已可返回角色和权限
- 全部 `/system/*` 控制器已完成 `@SaCheckPermission`
- 日志控制器已完成“仅读删”收口
- 单元测试与集成测试通过
