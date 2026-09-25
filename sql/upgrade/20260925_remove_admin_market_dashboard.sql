-- 行情快照 GET/SSE 已迁至 vita-openapi 匿名只读接口。
-- 清理旧 admin 菜单、权限及其角色关联；按业务键查找，可重复执行。
SET @market_permission_id := (SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'market:dashboard:view'
    LIMIT 1);
SET @market_overview_id := (SELECT `id` FROM `sys_menu`
    WHERE `route_name` = 'MarketOverview' AND `route_link` = '/market/overview'
    LIMIT 1);
SET @market_root_id := (SELECT `id` FROM `sys_menu`
    WHERE `route_name` = 'Market' AND `route_link` = '/market'
    LIMIT 1);

DELETE FROM `sys_role_permission` WHERE `permission_id` = @market_permission_id;
DELETE FROM `sys_permission` WHERE `id` = @market_permission_id;
DELETE FROM `sys_role_menu` WHERE `menu_id` = @market_overview_id;
DELETE FROM `sys_menu` WHERE `id` = @market_overview_id;

-- 根菜单若已承载其他子菜单则保留，不影响新增业务。
SET @market_root_children := (SELECT COUNT(*) FROM `sys_menu`
    WHERE `parent_id` = @market_root_id AND `is_deleted` = 0);
DELETE FROM `sys_role_menu`
    WHERE `menu_id` = @market_root_id AND @market_root_children = 0;
DELETE FROM `sys_menu`
    WHERE `id` = @market_root_id AND @market_root_children = 0;
