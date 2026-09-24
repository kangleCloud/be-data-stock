-- AKShare 市场快照 V1 行情总览菜单与只读权限；可重复执行。
-- 在 20260807_remove_hot_market.sql 之后执行，不恢复旧股票监控菜单。
SET NAMES utf8mb4;

SET @market_menu_id := (SELECT `id` FROM `sys_menu`
    WHERE `route_name` = 'Market' AND `is_deleted` = 0 LIMIT 1);
SET @next_menu_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_menu`);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`,
    `component_path`, `icon`, `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`,
    `status`, `is_system`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`,
    `update_time`, `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_menu_id := @next_menu_id + 1, 0, 'A股行情', 'CONTENTS', 'Market', '/market',
    'MarketLayout', 'trendCharts', 20, 1, 0, 1, 0, 1, 1, 0, 0, NOW(), 1, 'system',
    NOW(), 1, 'system', 0, 'AKShare 市场快照 V1'
WHERE @market_menu_id IS NULL;
SET @market_menu_id := (SELECT `id` FROM `sys_menu`
    WHERE `route_name` = 'Market' AND `is_deleted` = 0 LIMIT 1);

SET @market_overview_id := (SELECT `id` FROM `sys_menu`
    WHERE `route_name` = 'MarketOverview' AND `is_deleted` = 0 LIMIT 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`,
    `component_path`, `icon`, `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`,
    `status`, `is_system`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`,
    `update_time`, `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_menu_id := @next_menu_id + 1, @market_menu_id, '大盘与板块总览', 'MENU',
    'MarketOverview', '/market/overview', 'market/overview/index', 'dataAnalysis',
    21, 1, 0, 0, 0, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0,
    '仅展示热力图、板块 Top5 和大盘资金流'
WHERE @market_overview_id IS NULL;
SET @market_overview_id := (SELECT `id` FROM `sys_menu`
    WHERE `route_name` = 'MarketOverview' AND `is_deleted` = 0 LIMIT 1);

SET @market_permission_id := (SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'market:dashboard:view' AND `is_deleted` = 0 LIMIT 1);
SET @next_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_permission`);
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`,
    `menu_id`, `api_method`, `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`,
    `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`,
    `update_time`, `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '行情总览查看',
    'market:dashboard:view', 'MENU_ACTION', @market_overview_id, 'GET',
    '/market/dashboard/snapshot', 'market:dashboard:view', 21, 1, 1, 0, 0,
    NOW(), 1, 'system', NOW(), 1, 'system', 0, 'AKShare 市场快照 V1 只读权限'
WHERE @market_permission_id IS NULL;
SET @market_permission_id := (SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'market:dashboard:view' AND `is_deleted` = 0 LIMIT 1);

SET @next_role_menu_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role_menu`);
SET @next_role_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role_permission`);
-- 已有角色沿用原 RBAC 授权关系；默认给超级管理员与普通用户开放只读总览。
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`,
    `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id := @next_role_menu_id + 1, r.`id`, @market_menu_id, 0, 0,
    NOW(), 1, 'system', NOW(), 1, 'system', 0
FROM `sys_role` r
WHERE r.`role_code` IN ('SUPER_ADMIN', 'COMMON_USER') AND r.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` linked WHERE linked.`role_id` = r.`id`
    AND linked.`menu_id` = @market_menu_id AND linked.`is_deleted` = 0);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`,
    `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_menu_id := @next_role_menu_id + 1, r.`id`, @market_overview_id, 0, 0,
    NOW(), 1, 'system', NOW(), 1, 'system', 0
FROM `sys_role` r
WHERE r.`role_code` IN ('SUPER_ADMIN', 'COMMON_USER') AND r.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` linked WHERE linked.`role_id` = r.`id`
    AND linked.`menu_id` = @market_overview_id AND linked.`is_deleted` = 0);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`,
    `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1, r.`id`, @market_permission_id,
    0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
FROM `sys_role` r
WHERE r.`role_code` IN ('SUPER_ADMIN', 'COMMON_USER') AND r.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_permission` linked WHERE linked.`role_id` = r.`id`
    AND linked.`permission_id` = @market_permission_id AND linked.`is_deleted` = 0);
