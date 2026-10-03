-- ETF V1：仅保存字典、配置、资料和真实取得的资产配置；行情与曲线留在 Redis。
CREATE TABLE IF NOT EXISTS `etf_symbol_dictionary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `symbol` varchar(16) NOT NULL COMMENT 'SH/SZ 前缀 ETF 标识',
  `code` varchar(6) NOT NULL COMMENT '六位代码',
  `name` varchar(100) NOT NULL COMMENT '基金名称',
  `market` varchar(2) NOT NULL COMMENT 'SH/SZ',
  `exchange` varchar(20) DEFAULT NULL COMMENT '交易所',
  `etf_type` varchar(100) DEFAULT NULL COMMENT 'ETF 类型',
  `listing_status` varchar(30) DEFAULT NULL COMMENT '上市状态',
  `listing_date` date DEFAULT NULL COMMENT '上市日期',
  `tracking_index_code` varchar(32) DEFAULT NULL COMMENT '已核实的跟踪指数代码',
  `tracking_index_name` varchar(100) DEFAULT NULL COMMENT '已核实的跟踪指数名称',
  `source` varchar(50) NOT NULL COMMENT '字典来源',
  `synced_at` datetime DEFAULT NULL COMMENT '实际同步时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
  `create_by_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `version` bigint DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_etf_symbol_dictionary_symbol` (`symbol`),
  KEY `idx_etf_symbol_dictionary_code` (`code`),
  KEY `idx_etf_symbol_dictionary_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='新浪 ETF 交易字典';

CREATE TABLE IF NOT EXISTS `etf_monitor_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `symbol` varchar(16) NOT NULL COMMENT 'ETF 标识',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
  `create_by_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `version` bigint DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_etf_monitor_config_symbol` (`symbol`),
  KEY `idx_etf_monitor_config_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统级 ETF 监控配置';

CREATE TABLE IF NOT EXISTS `etf_monitor_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `symbol` varchar(16) NOT NULL COMMENT 'ETF 标识',
  `exchange` varchar(20) DEFAULT NULL COMMENT '交易所',
  `etf_type` varchar(100) DEFAULT NULL COMMENT 'ETF 类型',
  `listing_status` varchar(30) DEFAULT NULL COMMENT '上市状态',
  `listing_date` date DEFAULT NULL COMMENT '上市日期',
  `manager` varchar(100) DEFAULT NULL COMMENT '基金管理人',
  `custodian` varchar(100) DEFAULT NULL COMMENT '托管人',
  `share_count` decimal(24,4) DEFAULT NULL COMMENT '份额 原值',
  `share_date` date DEFAULT NULL COMMENT '份额数据日期',
  `tracking_index_code` varchar(32) DEFAULT NULL COMMENT '已核实的跟踪指数代码',
  `tracking_index_name` varchar(100) DEFAULT NULL COMMENT '已核实的跟踪指数名称',
  `profile_updated_at` datetime DEFAULT NULL COMMENT '实际资料同步时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
  `create_by_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `version` bigint DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_etf_monitor_profile_symbol` (`symbol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='ETF 基础资料';

CREATE TABLE IF NOT EXISTS `etf_asset_allocation_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `symbol` varchar(16) NOT NULL COMMENT 'ETF 标识',
  `requested_report_period` date NOT NULL COMMENT '请求报告期 非实际披露日',
  `source` varchar(30) NOT NULL COMMENT 'XQ_DANJUAN',
  `collected_at` datetime NOT NULL COMMENT '实际采集时间',
  `categories_json` json NOT NULL COMMENT '真实资产类别及占比',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
  `create_by_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `version` bigint DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_etf_asset_allocation_period` (`symbol`, `requested_report_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='ETF 真实资产类别占比报告';

CREATE TABLE IF NOT EXISTS `market_index_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `code` varchar(16) NOT NULL COMMENT '新浪指数代码',
  `name` varchar(30) NOT NULL COMMENT '指数名称',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启用',
  `sort_order` int NOT NULL COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
  `create_by_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by_id` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `version` bigint DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_market_index_config_code` (`code`),
  KEY `idx_market_index_config_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='五核心指数显示配置';

INSERT INTO `market_index_config` (`code`,`name`,`enabled`,`sort_order`)
VALUES ('sh000001','上证指数',1,1),('sz399001','深证成指',1,2),
       ('sh000300','沪深300',1,3),('sz399006','创业板指',1,4),('sh000688','科创50',1,5)
ON DUPLICATE KEY UPDATE `is_deleted`=0;

-- 系统菜单与精确权限；按实际系统根目录挂载，重复执行补缺并修正路由元数据。
-- 前置条件：已有未删除的 /system CONTENTS 根目录；保留已有菜单的启停和可见性设置。
SET @etf_system_menu_id := (SELECT `id` FROM `sys_menu`
 WHERE `route_link`='/system' AND `menu_type`='CONTENTS' AND `is_deleted`=0
 ORDER BY `id` LIMIT 1);
SET @next_etf_menu_id := (SELECT COALESCE(MAX(`id`),0) FROM `sys_menu`);
INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`route_name`,`route_link`,`component_path`,
 `icon`,`sort_no`,`visible`,`is_cache`,`always_show`,`is_external`,`status`,`is_system`,
 `is_deleted`,`tenant_id`,`create_time`,`create_by_id`,`create_by`,`update_time`,
 `update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_menu_id := @next_etf_menu_id + 1, @etf_system_menu_id, 'ETF监控', 'MENU', 'EtfMonitor', '/system/etfMonitor',
 'system/etfMonitor/index', 'list', 17, 1,0,0,0,1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE (`route_name`='EtfMonitor' OR `route_link`='/system/etfMonitor') AND `is_deleted`=0);
UPDATE `sys_menu` SET `parent_id`=@etf_system_menu_id, `menu_type`='MENU',
 `route_name`='EtfMonitor', `route_link`='/system/etfMonitor', `component_path`='system/etfMonitor/index',
 `is_external`=0, `update_time`=NOW()
WHERE (`route_name`='EtfMonitor' OR `route_link`='/system/etfMonitor') AND `is_deleted`=0;
INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`route_name`,`route_link`,`component_path`,
 `icon`,`sort_no`,`visible`,`is_cache`,`always_show`,`is_external`,`status`,`is_system`,
 `is_deleted`,`tenant_id`,`create_time`,`create_by_id`,`create_by`,`update_time`,
 `update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_menu_id := @next_etf_menu_id + 1, @etf_system_menu_id, 'ETF字典', 'MENU', 'EtfDictionary', '/system/etfDictionary',
 'system/etfDictionary/index', 'list', 18, 1,0,0,0,1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE (`route_name`='EtfDictionary' OR `route_link`='/system/etfDictionary') AND `is_deleted`=0);
UPDATE `sys_menu` SET `parent_id`=@etf_system_menu_id, `menu_type`='MENU',
 `route_name`='EtfDictionary', `route_link`='/system/etfDictionary', `component_path`='system/etfDictionary/index',
 `is_external`=0, `update_time`=NOW()
WHERE (`route_name`='EtfDictionary' OR `route_link`='/system/etfDictionary') AND `is_deleted`=0;
INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`route_name`,`route_link`,`component_path`,
 `icon`,`sort_no`,`visible`,`is_cache`,`always_show`,`is_external`,`status`,`is_system`,
 `is_deleted`,`tenant_id`,`create_time`,`create_by_id`,`create_by`,`update_time`,
 `update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_menu_id := @next_etf_menu_id + 1, @etf_system_menu_id, 'ETF资料', 'MENU', 'EtfProfile', '/system/etfProfile',
 'system/etfProfile/index', 'list', 19, 1,0,0,0,1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE (`route_name`='EtfProfile' OR `route_link`='/system/etfProfile') AND `is_deleted`=0);
UPDATE `sys_menu` SET `parent_id`=@etf_system_menu_id, `menu_type`='MENU',
 `route_name`='EtfProfile', `route_link`='/system/etfProfile', `component_path`='system/etfProfile/index',
 `is_external`=0, `update_time`=NOW()
WHERE (`route_name`='EtfProfile' OR `route_link`='/system/etfProfile') AND `is_deleted`=0;
INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`route_name`,`route_link`,`component_path`,
 `icon`,`sort_no`,`visible`,`is_cache`,`always_show`,`is_external`,`status`,`is_system`,
 `is_deleted`,`tenant_id`,`create_time`,`create_by_id`,`create_by`,`update_time`,
 `update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_menu_id := @next_etf_menu_id + 1, @etf_system_menu_id, '核心指数配置', 'MENU', 'MarketIndexConfig', '/system/indexConfig',
 'system/indexConfig/index', 'list', 20, 1,0,0,0,1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE (`route_name`='MarketIndexConfig' OR `route_link`='/system/indexConfig') AND `is_deleted`=0);
UPDATE `sys_menu` SET `parent_id`=@etf_system_menu_id, `menu_type`='MENU',
 `route_name`='MarketIndexConfig', `route_link`='/system/indexConfig', `component_path`='system/indexConfig/index',
 `is_external`=0, `update_time`=NOW()
WHERE (`route_name`='MarketIndexConfig' OR `route_link`='/system/indexConfig') AND `is_deleted`=0;
SET @next_etf_permission_id := (SELECT COALESCE(MAX(`id`),0) FROM `sys_permission`);
INSERT INTO `sys_permission` (`id`,`permission_name`,`permission_code`,`permission_type`,`menu_id`,
 `api_method`,`api_path`,`auth_tag`,`sort_no`,`status`,`is_system`,`is_deleted`,`tenant_id`,
 `create_time`,`create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_permission_id := @next_etf_permission_id + 1, 'ETF监控查看', 'system:etf-monitor:view', 'MENU_ACTION',
 (SELECT `id` FROM `sys_menu` WHERE `route_name`='EtfMonitor' AND `is_deleted`=0 LIMIT 1),
 'GET', '/system/etfMonitor/list', 'system:etf-monitor:view', 121, 1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='system:etf-monitor:view' AND `is_deleted`=0);
UPDATE `sys_permission` SET `menu_id`=(SELECT `id` FROM `sys_menu`
 WHERE `route_name`='EtfMonitor' AND `is_deleted`=0 ORDER BY `id` LIMIT 1),
 `auth_tag`='system:etf-monitor:view', `api_method`='GET', `api_path`='/system/etfMonitor/list', `update_time`=NOW()
WHERE `permission_code`='system:etf-monitor:view' AND `is_deleted`=0;
INSERT INTO `sys_permission` (`id`,`permission_name`,`permission_code`,`permission_type`,`menu_id`,
 `api_method`,`api_path`,`auth_tag`,`sort_no`,`status`,`is_system`,`is_deleted`,`tenant_id`,
 `create_time`,`create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_permission_id := @next_etf_permission_id + 1, 'ETF监控修改', 'system:etf-monitor:update', 'MENU_ACTION',
 (SELECT `id` FROM `sys_menu` WHERE `route_name`='EtfMonitor' AND `is_deleted`=0 LIMIT 1),
 'POST', '/system/etfMonitor/**', 'system:etf-monitor:update', 122, 1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='system:etf-monitor:update' AND `is_deleted`=0);
UPDATE `sys_permission` SET `menu_id`=(SELECT `id` FROM `sys_menu`
 WHERE `route_name`='EtfMonitor' AND `is_deleted`=0 ORDER BY `id` LIMIT 1),
 `auth_tag`='system:etf-monitor:update', `api_method`='POST', `api_path`='/system/etfMonitor/**', `update_time`=NOW()
WHERE `permission_code`='system:etf-monitor:update' AND `is_deleted`=0;
INSERT INTO `sys_permission` (`id`,`permission_name`,`permission_code`,`permission_type`,`menu_id`,
 `api_method`,`api_path`,`auth_tag`,`sort_no`,`status`,`is_system`,`is_deleted`,`tenant_id`,
 `create_time`,`create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_permission_id := @next_etf_permission_id + 1, 'ETF监控刷新', 'system:etf-monitor:refresh', 'MENU_ACTION',
 (SELECT `id` FROM `sys_menu` WHERE `route_name`='EtfMonitor' AND `is_deleted`=0 LIMIT 1),
 'POST', '/system/etfMonitor/**', 'system:etf-monitor:refresh', 123, 1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='system:etf-monitor:refresh' AND `is_deleted`=0);
UPDATE `sys_permission` SET `menu_id`=(SELECT `id` FROM `sys_menu`
 WHERE `route_name`='EtfMonitor' AND `is_deleted`=0 ORDER BY `id` LIMIT 1),
 `auth_tag`='system:etf-monitor:refresh', `api_method`='POST', `api_path`='/system/etfMonitor/**', `update_time`=NOW()
WHERE `permission_code`='system:etf-monitor:refresh' AND `is_deleted`=0;
INSERT INTO `sys_permission` (`id`,`permission_name`,`permission_code`,`permission_type`,`menu_id`,
 `api_method`,`api_path`,`auth_tag`,`sort_no`,`status`,`is_system`,`is_deleted`,`tenant_id`,
 `create_time`,`create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_permission_id := @next_etf_permission_id + 1, 'ETF字典查看', 'system:etf-dictionary:view', 'MENU_ACTION',
 (SELECT `id` FROM `sys_menu` WHERE `route_name`='EtfDictionary' AND `is_deleted`=0 LIMIT 1),
 'GET', '/system/etfDictionary/page', 'system:etf-dictionary:view', 124, 1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='system:etf-dictionary:view' AND `is_deleted`=0);
UPDATE `sys_permission` SET `menu_id`=(SELECT `id` FROM `sys_menu`
 WHERE `route_name`='EtfDictionary' AND `is_deleted`=0 ORDER BY `id` LIMIT 1),
 `auth_tag`='system:etf-dictionary:view', `api_method`='GET', `api_path`='/system/etfDictionary/page', `update_time`=NOW()
WHERE `permission_code`='system:etf-dictionary:view' AND `is_deleted`=0;
INSERT INTO `sys_permission` (`id`,`permission_name`,`permission_code`,`permission_type`,`menu_id`,
 `api_method`,`api_path`,`auth_tag`,`sort_no`,`status`,`is_system`,`is_deleted`,`tenant_id`,
 `create_time`,`create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_permission_id := @next_etf_permission_id + 1, 'ETF资料查看', 'system:etf-profile:view', 'MENU_ACTION',
 (SELECT `id` FROM `sys_menu` WHERE `route_name`='EtfProfile' AND `is_deleted`=0 LIMIT 1),
 'GET', '/system/etfProfile/**', 'system:etf-profile:view', 125, 1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='system:etf-profile:view' AND `is_deleted`=0);
UPDATE `sys_permission` SET `menu_id`=(SELECT `id` FROM `sys_menu`
 WHERE `route_name`='EtfProfile' AND `is_deleted`=0 ORDER BY `id` LIMIT 1),
 `auth_tag`='system:etf-profile:view', `api_method`='GET', `api_path`='/system/etfProfile/**', `update_time`=NOW()
WHERE `permission_code`='system:etf-profile:view' AND `is_deleted`=0;
INSERT INTO `sys_permission` (`id`,`permission_name`,`permission_code`,`permission_type`,`menu_id`,
 `api_method`,`api_path`,`auth_tag`,`sort_no`,`status`,`is_system`,`is_deleted`,`tenant_id`,
 `create_time`,`create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_permission_id := @next_etf_permission_id + 1, '核心指数配置查看', 'system:index-config:view', 'MENU_ACTION',
 (SELECT `id` FROM `sys_menu` WHERE `route_name`='MarketIndexConfig' AND `is_deleted`=0 LIMIT 1),
 'GET', '/system/indexConfig/list', 'system:index-config:view', 126, 1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='system:index-config:view' AND `is_deleted`=0);
UPDATE `sys_permission` SET `menu_id`=(SELECT `id` FROM `sys_menu`
 WHERE `route_name`='MarketIndexConfig' AND `is_deleted`=0 ORDER BY `id` LIMIT 1),
 `auth_tag`='system:index-config:view', `api_method`='GET', `api_path`='/system/indexConfig/list', `update_time`=NOW()
WHERE `permission_code`='system:index-config:view' AND `is_deleted`=0;
INSERT INTO `sys_permission` (`id`,`permission_name`,`permission_code`,`permission_type`,`menu_id`,
 `api_method`,`api_path`,`auth_tag`,`sort_no`,`status`,`is_system`,`is_deleted`,`tenant_id`,
 `create_time`,`create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`,`remark`)
SELECT @next_etf_permission_id := @next_etf_permission_id + 1, '核心指数配置修改', 'system:index-config:update', 'MENU_ACTION',
 (SELECT `id` FROM `sys_menu` WHERE `route_name`='MarketIndexConfig' AND `is_deleted`=0 LIMIT 1),
 'POST', '/system/indexConfig/update', 'system:index-config:update', 127, 1,1,0,0,NOW(),1,'system',NOW(),1,'system',0,'三大屏 V1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='system:index-config:update' AND `is_deleted`=0);
UPDATE `sys_permission` SET `menu_id`=(SELECT `id` FROM `sys_menu`
 WHERE `route_name`='MarketIndexConfig' AND `is_deleted`=0 ORDER BY `id` LIMIT 1),
 `auth_tag`='system:index-config:update', `api_method`='POST', `api_path`='/system/indexConfig/update', `update_time`=NOW()
WHERE `permission_code`='system:index-config:update' AND `is_deleted`=0;
SET @etf_super_admin_role_id := (SELECT `id` FROM `sys_role` WHERE `role_code`='SUPER_ADMIN' AND `is_deleted`=0 LIMIT 1);
SET @next_etf_role_menu_id := (SELECT COALESCE(MAX(`id`),0) FROM `sys_role_menu`);
INSERT INTO `sys_role_menu` (`id`,`role_id`,`menu_id`,`is_deleted`,`tenant_id`,`create_time`,
 `create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`)
SELECT @next_etf_role_menu_id := @next_etf_role_menu_id + 1,
 @etf_super_admin_role_id,m.`id`,0,0,NOW(),1,'system',NOW(),1,'system',0
FROM `sys_menu` m WHERE @etf_super_admin_role_id IS NOT NULL
 AND (m.`id`=@etf_system_menu_id OR m.`route_name` IN ('EtfMonitor','EtfDictionary','EtfProfile','MarketIndexConfig'))
 AND m.`is_deleted`=0 AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm
 WHERE rm.`role_id`=@etf_super_admin_role_id AND rm.`menu_id`=m.`id` AND rm.`is_deleted`=0);
SET @next_etf_role_permission_id := (SELECT COALESCE(MAX(`id`),0) FROM `sys_role_permission`);
INSERT INTO `sys_role_permission` (`id`,`role_id`,`permission_id`,`is_deleted`,`tenant_id`,
 `create_time`,`create_by_id`,`create_by`,`update_time`,`update_by_id`,`update_by`,`version`)
SELECT @next_etf_role_permission_id := @next_etf_role_permission_id + 1,
 @etf_super_admin_role_id,p.`id`,0,0,NOW(),1,'system',NOW(),1,'system',0
FROM `sys_permission` p WHERE @etf_super_admin_role_id IS NOT NULL
 AND p.`permission_code` IN ('system:etf-monitor:view','system:etf-monitor:update','system:etf-monitor:refresh','system:etf-dictionary:view','system:etf-profile:view','system:index-config:view','system:index-config:update' ) AND p.`is_deleted`=0 AND NOT EXISTS (SELECT 1 FROM `sys_role_permission` rp
 WHERE rp.`role_id`=@etf_super_admin_role_id AND rp.`permission_id`=p.`id` AND rp.`is_deleted`=0);
