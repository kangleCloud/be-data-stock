-- =============================================
-- File: 20260807_remove_hot_market.sql
-- Desc: 移除热搜与 A 股行情遗留数据
-- Risk: 本脚本会永久删除行情历史数据，执行前必须完成数据库备份
-- Note: Redis 中 hot:*、market:* 缓存需要在发布时由运维人工清理
-- =============================================

SET NAMES utf8mb4;

-- 先清理角色授权，避免保留指向已下线权限的关系数据。
DELETE FROM `sys_role_permission`
WHERE `permission_id` IN (
    SELECT `id`
    FROM `sys_permission`
    WHERE `permission_code` IN (
        'system:hot-cache:view',
        'system:hot-cache:refresh',
        'market:dashboard:view',
        'market:stock:manage',
        'market:stock:sync'
    )
);

DELETE FROM `sys_permission`
WHERE `permission_code` IN (
    'system:hot-cache:view',
    'system:hot-cache:refresh',
    'market:dashboard:view',
    'market:stock:manage',
    'market:stock:sync'
);

-- 行情菜单按关联关系、子菜单、根目录的顺序清理。
DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (
    SELECT `id`
    FROM `sys_menu`
    WHERE `route_name` IN ('MarketOverview', 'MarketStockMonitor', 'Market')
);

DELETE FROM `sys_menu`
WHERE `route_name` IN ('MarketOverview', 'MarketStockMonitor');

DELETE FROM `sys_menu`
WHERE `route_name` = 'Market';

-- 行情采样明细先于主数据删除，脚本可安全重复执行。
DROP TABLE IF EXISTS `market_stock_fund_point`;
DROP TABLE IF EXISTS `market_daily_summary`;
DROP TABLE IF EXISTS `market_stock`;
