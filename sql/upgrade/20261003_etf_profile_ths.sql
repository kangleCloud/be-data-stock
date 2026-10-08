-- ETF 基本资料切换同花顺：先备份并执行本表重建脚本，再启动匹配的应用版本。
-- 用户要求删除旧表重新构建；执行会删除 etf_monitor_profile 全部资料，包括已采集的 THS 数据。
-- 重建后通过资料刷新重新采集；重复执行仍会清空全部资料，不作为可保留数据的增量迁移。
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `etf_monitor_profile`;

CREATE TABLE `etf_monitor_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `symbol` varchar(16) NOT NULL COMMENT 'ETF 标识',
  `exchange` varchar(20) DEFAULT NULL COMMENT '交易所',
  `etf_type` varchar(100) DEFAULT NULL COMMENT 'ETF 类型',
  `full_name` varchar(200) DEFAULT NULL COMMENT '基金全称',
  `fund_type` varchar(100) DEFAULT NULL COMMENT '同花顺基金类型 非字典ETF分类',
  `investment_type` varchar(100) DEFAULT NULL COMMENT '投资类型',
  `fund_manager` varchar(200) DEFAULT NULL COMMENT '基金经理 非基金管理人',
  `established_date` date DEFAULT NULL COMMENT '成立日期 非上市日期',
  `performance_benchmark` varchar(1000) DEFAULT NULL COMMENT '业绩比较基准原文 不推断指数代码',
  `source` varchar(30) DEFAULT NULL COMMENT '同花顺基本资料来源 THS',
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
