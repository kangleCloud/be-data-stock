-- 个股监控 V1 增量升级；完整字段及索引与 sql/init/system.sql 保持一致。
CREATE TABLE IF NOT EXISTS `stock_symbol_dictionary` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
    `symbol` varchar(16) NOT NULL COMMENT '交易所前缀股票标识',
    `code` varchar(12) NOT NULL COMMENT '股票代码',
    `name` varchar(100) NOT NULL COMMENT '股票名称',
    `market` varchar(2) NOT NULL COMMENT 'SH/SZ/BJ',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
    `create_by_id` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `update_by_id` bigint DEFAULT NULL COMMENT '更新人ID',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `version` bigint DEFAULT 0 COMMENT '版本号',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_symbol_dictionary_symbol` (`symbol`),
    KEY `idx_stock_symbol_dictionary_code` (`code`),
    KEY `idx_stock_symbol_dictionary_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='交易所股票字典';

CREATE TABLE IF NOT EXISTS `stock_monitor_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
    `symbol` varchar(16) NOT NULL COMMENT '股票标识',
    `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '监控启用状态',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
    `create_by_id` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `update_by_id` bigint DEFAULT NULL COMMENT '更新人ID',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `version` bigint DEFAULT 0 COMMENT '版本号',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_monitor_config_symbol` (`symbol`),
    KEY `idx_stock_monitor_config_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统级个股监控配置';

CREATE TABLE IF NOT EXISTS `stock_monitor_profile` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
    `symbol` varchar(16) NOT NULL COMMENT '股票标识',
    `industry` varchar(100) DEFAULT NULL COMMENT '所属行业',
    `listing_date` varchar(10) DEFAULT NULL COMMENT '上市日期 YYYY-MM-DD',
    `market_cap` decimal(22,2) DEFAULT NULL COMMENT '总市值 元',
    `profile_updated_at` varchar(35) DEFAULT NULL COMMENT '资料更新时间 带时区偏移 ISO 8601',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
    `create_by_id` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `update_by_id` bigint DEFAULT NULL COMMENT '更新人ID',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `version` bigint DEFAULT 0 COMMENT '版本号',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_monitor_profile_symbol` (`symbol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选中个股雪球有限基础资料';

-- 三个独立系统管理菜单；使用当前最大 ID，避免与已有环境的自定义菜单冲突。
SET @next_stock_menu_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_menu`);

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`, `component_path`,
                        `icon`, `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`, `status`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_stock_menu_id := @next_stock_menu_id + 1,
       1000, '个股监控', 'MENU', 'StockMonitor', '/system/stockMonitor', 'system/stockMonitor/index',
       'monitor', 14, 1, 0, 0, 0, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, '系统级个股监控配置'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `route_name` = 'StockMonitor' AND `is_deleted` = 0);

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`, `component_path`,
                        `icon`, `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`, `status`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_stock_menu_id := @next_stock_menu_id + 1,
       1000, '股票字典', 'MENU', 'StockDictionary', '/system/stockDictionary', 'system/stockDictionary/index',
       'list', 15, 1, 0, 0, 0, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, '交易所股票字典'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `route_name` = 'StockDictionary' AND `is_deleted` = 0);

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `route_name`, `route_link`, `component_path`,
                        `icon`, `sort_no`, `visible`, `is_cache`, `always_show`, `is_external`, `status`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_stock_menu_id := @next_stock_menu_id + 1,
       1000, '股票资料', 'MENU', 'StockProfile', '/system/stockProfile', 'system/stockProfile/index',
       'document', 16, 1, 0, 0, 0, 1, 1, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0, '已同步的股票基础资料'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `route_name` = 'StockProfile' AND `is_deleted` = 0);

SET @stock_monitor_menu_id := (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'StockMonitor' AND `is_deleted` = 0 LIMIT 1);
SET @stock_dictionary_menu_id := (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'StockDictionary' AND `is_deleted` = 0 LIMIT 1);
SET @stock_profile_menu_id := (SELECT `id` FROM `sys_menu` WHERE `route_name` = 'StockProfile' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
VALUES (2045, '个股监控查看', 'system:stock-monitor:view', 'MENU_ACTION', @stock_monitor_menu_id, 'GET',
        '/system/stockMonitor/**', 'system:stock-monitor:view', 114, 1, 1, 0, 0,
        '2026-09-28 00:00:00', 1, 'system', '2026-09-28 00:00:00', 1, 'system',
        0, '查看系统级个股监控配置'),
       (2046, '个股监控修改', 'system:stock-monitor:update', 'MENU_ACTION', @stock_monitor_menu_id, 'POST',
        '/system/stockMonitor/**', 'system:stock-monitor:update', 115, 1, 1, 0, 0,
        '2026-09-28 00:00:00', 1, 'system', '2026-09-28 00:00:00', 1, 'system',
        0, '启停和排序个股监控清单'),
       (2047, '个股监控刷新', 'system:stock-monitor:refresh', 'MENU_ACTION', @stock_monitor_menu_id, 'POST',
        '/system/stockMonitor/refresh', 'system:stock-monitor:refresh', 116, 1, 1, 0, 0,
        '2026-09-28 00:00:00', 1, 'system', '2026-09-28 00:00:00', 1, 'system',
        0, '触发交易所字典及允许时的雪球资料刷新')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), menu_id = VALUES(menu_id),
                        api_path = VALUES(api_path), auth_tag = VALUES(auth_tag), remark = VALUES(remark);

SET @next_stock_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_permission`);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_stock_permission_id := @next_stock_permission_id + 1,
       '股票字典查看', 'system:stock-dictionary:view', 'MENU_ACTION', @stock_dictionary_menu_id, 'GET',
       '/system/stockDictionary/page', 'system:stock-dictionary:view', 117, 1, 1, 0, 0,
       NOW(), 1, 'system', NOW(), 1, 'system', 0, '分页查询交易所股票字典'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code` = 'system:stock-dictionary:view' AND `is_deleted` = 0);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_stock_permission_id := @next_stock_permission_id + 1,
       '股票资料查看', 'system:stock-profile:view', 'MENU_ACTION', @stock_profile_menu_id, 'GET',
       '/system/stockProfile/page', 'system:stock-profile:view', 118, 1, 1, 0, 0,
       NOW(), 1, 'system', NOW(), 1, 'system', 0, '分页查询已同步股票资料'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code` = 'system:stock-profile:view' AND `is_deleted` = 0);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_stock_permission_id := @next_stock_permission_id + 1,
       '股票字典新增', 'system:stock-dictionary:add', 'MENU_ACTION', @stock_dictionary_menu_id, 'POST',
       '/system/stockDictionary/add', 'system:stock-dictionary:add', 119, 1, 1, 0, 0,
       NOW(), 1, 'system', NOW(), 1, 'system', 0, '手工补录一只交易所股票'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code` = 'system:stock-dictionary:add' AND `is_deleted` = 0);

SET @next_stock_role_menu_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role_menu`);
SET @stock_super_admin_role_id := (SELECT `id` FROM `sys_role`
                                   WHERE `role_code` = 'SUPER_ADMIN' AND `is_deleted` = 0 LIMIT 1);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `is_deleted`, `tenant_id`, `create_time`, `create_by_id`,
                             `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_stock_role_menu_id := @next_stock_role_menu_id + 1,
       @stock_super_admin_role_id, m.`id`, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
FROM `sys_menu` m
WHERE @stock_super_admin_role_id IS NOT NULL
  AND m.`route_name` IN ('StockMonitor', 'StockDictionary', 'StockProfile') AND m.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = @stock_super_admin_role_id
                  AND rm.`menu_id` = m.`id` AND rm.`is_deleted` = 0);

SET @next_stock_role_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_stock_role_permission_id := @next_stock_role_permission_id + 1,
       @stock_super_admin_role_id, p.`id`, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
FROM `sys_permission` p
WHERE @stock_super_admin_role_id IS NOT NULL AND p.`permission_code` IN (
    'system:stock-monitor:view', 'system:stock-monitor:update', 'system:stock-monitor:refresh',
    'system:stock-dictionary:view', 'system:stock-dictionary:add', 'system:stock-profile:view')
  AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `sys_role_permission` rp WHERE rp.`role_id` = @stock_super_admin_role_id
                  AND rp.`permission_id` = p.`id` AND rp.`is_deleted` = 0);
