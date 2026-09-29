-- =============================================
-- File: vita_workflow_upgrade.sql
-- Desc: be-vita 工作流扩展表与 RBAC 数据增量升级脚本
-- Warning: 本脚本不会删除工作流扩展表和业务数据，可重复执行。
-- Order: 常规升级只执行本脚本；warm_flow_upgrade.sql 仅用于明确接受数据清空的人工重建。
-- =============================================

SET NAMES utf8mb4;

-- ----------------------------
-- be-vita workflow extension table
-- ----------------------------
CREATE TABLE IF NOT EXISTS `workflow_category`
(
    `id`            bigint       NOT NULL COMMENT '分类ID',
    `tenant_id`     bigint                DEFAULT NULL COMMENT '租户ID',
    `category_code` varchar(64)  NOT NULL COMMENT '分类编码',
    `category_name` varchar(100) NOT NULL COMMENT '分类名称',
    `parent_code`   varchar(64)           DEFAULT NULL COMMENT '父分类编码',
    `sort_no`       int          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`        tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`   datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    tinyint(1)            DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`  bigint                DEFAULT NULL COMMENT '新增用户ID',
    `create_by`     varchar(50)           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`  bigint                DEFAULT NULL COMMENT '更新用户ID',
    `update_by`     varchar(50)           DEFAULT NULL COMMENT '更新用户名称',
    `version`       bigint                DEFAULT '0' COMMENT '乐观锁',
    `remark`        varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_category_code` (`category_code`, `is_deleted`),
    KEY `idx_workflow_category_parent` (`parent_code`),
    KEY `idx_workflow_category_status_sort` (`status`, `sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工作流分类表';

SET @next_workflow_category_id := (
    SELECT COALESCE(MAX(`id`), 0) FROM `workflow_category`
);

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_workflow_category_id := @next_workflow_category_id + 1,
       0, 'FINANCE', '财务类', NULL, 10, 1, '2026-07-07 00:00:00', '2026-07-07 00:00:00',
       0, 1, 'system', 1, 'system', 0, '工作流默认分类'
WHERE NOT EXISTS (
    SELECT 1 FROM `workflow_category`
    WHERE `category_code` = 'FINANCE' AND `is_deleted` = 0
);

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_workflow_category_id := @next_workflow_category_id + 1,
       0, 'REIMBURSE', '报销审批', 'FINANCE', 11, 1, '2026-07-07 00:00:00', '2026-07-07 00:00:00',
       0, 1, 'system', 1, 'system', 0, '报销审批流程分类'
WHERE NOT EXISTS (
    SELECT 1 FROM `workflow_category`
    WHERE `category_code` = 'REIMBURSE' AND `is_deleted` = 0
);

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_workflow_category_id := @next_workflow_category_id + 1,
       0, 'HR', '人事类', NULL, 20, 1, '2026-07-07 00:00:00', '2026-07-07 00:00:00',
       0, 1, 'system', 1, 'system', 0, '工作流默认分类'
WHERE NOT EXISTS (
    SELECT 1 FROM `workflow_category`
    WHERE `category_code` = 'HR' AND `is_deleted` = 0
);

INSERT INTO `workflow_category` (`id`, `tenant_id`, `category_code`, `category_name`, `parent_code`, `sort_no`,
                                 `status`, `create_time`, `update_time`, `is_deleted`, `create_by_id`, `create_by`,
                                 `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_workflow_category_id := @next_workflow_category_id + 1,
       0, 'ADMIN', '行政类', NULL, 30, 1, '2026-07-07 00:00:00', '2026-07-07 00:00:00',
       0, 1, 'system', 1, 'system', 0, '工作流默认分类'
WHERE NOT EXISTS (
    SELECT 1 FROM `workflow_category`
    WHERE `category_code` = 'ADMIN' AND `is_deleted` = 0
);

-- 报销业务表只做缺失补齐，不得在升级脚本中删除或重建。
CREATE TABLE IF NOT EXISTS `workflow_reimbursement`
(
    `id`                bigint         NOT NULL COMMENT '报销单ID',
    `tenant_id`         bigint                  DEFAULT NULL COMMENT '租户ID',
    `business_id`       varchar(40)    NOT NULL COMMENT '全局业务ID',
    `title`             varchar(100)   NOT NULL COMMENT '报销标题',
    `expense_type`      varchar(50)    NOT NULL COMMENT '费用类型',
    `expense_date`      date           NOT NULL COMMENT '费用发生日期',
    `total_amount`      decimal(16, 2) NOT NULL COMMENT '报销总金额',
    `reason`            varchar(1000)  NOT NULL COMMENT '报销事由',
    `applicant_id`      bigint         NOT NULL COMMENT '申请人ID',
    `applicant_dept_id` bigint                  DEFAULT NULL COMMENT '申请部门ID',
    `instance_id`       bigint                  DEFAULT NULL COMMENT '流程实例ID',
    `business_status`   varchar(20)    NOT NULL COMMENT '业务状态',
    `create_time`       datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`       datetime                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        tinyint(1)              DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`      bigint                  DEFAULT NULL COMMENT '新增用户ID',
    `create_by`         varchar(50)             DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`      bigint                  DEFAULT NULL COMMENT '更新用户ID',
    `update_by`         varchar(50)             DEFAULT NULL COMMENT '更新用户名称',
    `version`           bigint                  DEFAULT '0' COMMENT '乐观锁',
    `remark`            varchar(500)            DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_reimbursement_business_id` (`business_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工作流报销单';

-- 通用运行时扩展表只做缺失补齐，不删除已有业务摘要、抄送和规则数据。
CREATE TABLE IF NOT EXISTS `workflow_instance_metadata`
(
    `id`             bigint       NOT NULL COMMENT '元数据ID',
    `tenant_id`      bigint                DEFAULT NULL COMMENT '租户ID',
    `business_id`    varchar(40)  NOT NULL COMMENT '全局业务ID',
    `instance_id`    bigint       NOT NULL COMMENT '流程实例ID',
    `flow_code`      varchar(100) NOT NULL COMMENT '流程编码',
    `business_code`  varchar(100)          DEFAULT NULL COMMENT '业务类型编码',
    `business_title` varchar(500)          DEFAULT NULL COMMENT '业务标题快照',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`    datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint(1)            DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`   bigint                DEFAULT NULL COMMENT '新增用户ID',
    `create_by`      varchar(50)           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`   bigint                DEFAULT NULL COMMENT '更新用户ID',
    `update_by`      varchar(50)           DEFAULT NULL COMMENT '更新用户名称',
    `version`        bigint                DEFAULT '0' COMMENT '乐观锁',
    `remark`         varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_metadata_business` (`business_id`),
    UNIQUE KEY `uk_workflow_metadata_instance` (`instance_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工作流实例业务元数据';

CREATE TABLE IF NOT EXISTS `workflow_task_copy`
(
    `id`                bigint       NOT NULL COMMENT '抄送记录ID',
    `tenant_id`         bigint                DEFAULT NULL COMMENT '租户ID',
    `instance_id`       bigint       NOT NULL COMMENT '流程实例ID',
    `history_task_id`   bigint       NOT NULL COMMENT '来源历史任务ID',
    `business_id`       varchar(40)  NOT NULL COMMENT '全局业务ID',
    `recipient_user_id` bigint       NOT NULL COMMENT '抄送接收用户ID',
    `source_node_code`  varchar(100)          DEFAULT NULL COMMENT '来源节点编码',
    `source_node_name`  varchar(100)          DEFAULT NULL COMMENT '来源节点名称',
    `message`           varchar(500)          DEFAULT NULL COMMENT '抄送意见',
    `attachment_ref`    varchar(500)          DEFAULT NULL COMMENT '附件不透明引用',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`       datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        tinyint(1)            DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`      bigint                DEFAULT NULL COMMENT '新增用户ID',
    `create_by`         varchar(50)           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`      bigint                DEFAULT NULL COMMENT '更新用户ID',
    `update_by`         varchar(50)           DEFAULT NULL COMMENT '更新用户名称',
    `version`           bigint                DEFAULT '0' COMMENT '乐观锁',
    `remark`            varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_copy_task_user` (`history_task_id`, `recipient_user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工作流任务抄送记录';

CREATE TABLE IF NOT EXISTS `workflow_assignee_rule`
(
    `id`             bigint       NOT NULL COMMENT '规则ID',
    `tenant_id`      bigint                DEFAULT NULL COMMENT '租户ID',
    `rule_code`      varchar(64)  NOT NULL COMMENT '规则编码',
    `rule_name`      varchar(100) NOT NULL COMMENT '规则名称',
    `resolver_code`  varchar(64)  NOT NULL COMMENT '平台注册解析器编码',
    `argument_names` varchar(500)          DEFAULT NULL COMMENT '受控参数名，逗号分隔',
    `status`         tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`    datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint(1)            DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`   bigint                DEFAULT NULL COMMENT '新增用户ID',
    `create_by`      varchar(50)           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`   bigint                DEFAULT NULL COMMENT '更新用户ID',
    `update_by`      varchar(50)           DEFAULT NULL COMMENT '更新用户名称',
    `version`        bigint                DEFAULT '0' COMMENT '乐观锁',
    `remark`         varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_assignee_rule_code` (`rule_code`, `is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工作流受控办理人规则';

SET @next_assignee_rule_id := (SELECT COALESCE(MAX(`id`), 0) FROM `workflow_assignee_rule`);
INSERT INTO `workflow_assignee_rule` (`id`, `tenant_id`, `rule_code`, `rule_name`, `resolver_code`,
                                      `argument_names`, `status`, `create_time`, `update_time`, `is_deleted`,
                                      `create_by_id`, `create_by`, `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_assignee_rule_id := @next_assignee_rule_id + 1, 0,
       'DEPT_LEADER', '部门负责人', 'DEPARTMENT_LEADER', 'deptId', 1,
       '2026-07-23 00:00:00', '2026-07-23 00:00:00', 0,
       1, 'system', 1, 'system', 0, '平台内置受控办理人规则'
WHERE NOT EXISTS (
    SELECT 1 FROM `workflow_assignee_rule`
    WHERE `rule_code` = 'DEPT_LEADER' AND `is_deleted` = 0
);

-- ----------------------------
-- workflow roles
-- ----------------------------
SET @next_role_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role`);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '财务审批角色', 'FINANCE_APPROVER', 91, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程财务节点'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `role_code` = 'FINANCE_APPROVER' AND `is_deleted` = 0
);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '综合管理部审批角色', 'GENERAL_AFFAIRS_APPROVER', 92, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程综合管理部节点'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `role_code` = 'GENERAL_AFFAIRS_APPROVER' AND `is_deleted` = 0
);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '总经理审批角色', 'GENERAL_MANAGER_APPROVER', 93, 1, 'SELF', 0,
       0, 0, '2026-07-03 00:00:00', 1, 'system', '2026-07-03 00:00:00',
       1, 'system', 0, '用于报销审批流程总经理节点'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `role_code` = 'GENERAL_MANAGER_APPROVER' AND `is_deleted` = 0
);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `status`, `data_scope_type`, `is_system`,
                        `is_deleted`, `tenant_id`, `create_time`, `create_by_id`, `create_by`, `update_time`,
                        `update_by_id`, `update_by`, `version`, `remark`)
SELECT @next_role_id := @next_role_id + 1, '子管理员', 'SUB_ADMIN', 3, 1, 'SELF', 0,
       0, 0, '2026-07-07 00:00:00', 1, 'system', '2026-07-07 00:00:00',
       1, 'system', 0, '可查看全部权限并管理工作流'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `role_code` = 'SUB_ADMIN' AND `is_deleted` = 0
);

-- ----------------------------
-- workflow permissions
-- ----------------------------
SET @next_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_permission`);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流定义导入', 'system:workflow:import',
       'MENU_ACTION', NULL, 'POST', '/system/workflow/importJson', 'system:workflow:import',
       101, 1, 1, 0, 0, '2026-07-03 00:00:00', 1, 'system',
       '2026-07-03 00:00:00', 1, 'system', 0, '导入 Warm-Flow 流程定义 JSON'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:import' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流定义查看', 'system:workflow:definition:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/definitions/**', 'system:workflow:definition:view',
       102, 1, 1, 0, 0, '2026-07-20 00:00:00', 1, 'system',
       '2026-07-20 00:00:00', 1, 'system', 0, '查看工作流定义列表和详情'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:definition:view' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流定义管理', 'system:workflow:definition:manage',
       'MENU_ACTION', NULL, NULL, '/system/workflow/definitions/**', 'system:workflow:definition:manage',
       103, 1, 1, 0, 0, '2026-07-20 00:00:00', 1, 'system',
       '2026-07-20 00:00:00', 1, 'system', 0, '发布、取消发布、复制和删除工作流定义'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:definition:manage' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流实例查看', 'system:workflow:instance:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/instances/**', 'system:workflow:instance:view',
       104, 1, 1, 0, 0, '2026-07-20 00:00:00', 1, 'system',
       '2026-07-20 00:00:00', 1, 'system', 0, '查看工作流实例和流转轨迹'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:instance:view' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '报销工作流查看', 'system:workflow:reimbursement:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/reimbursements/**', 'system:workflow:reimbursement:view',
       105, 1, 1, 0, 0, '2026-07-20 00:00:00', 1, 'system',
       '2026-07-20 00:00:00', 1, 'system', 0, '查看报销工作流列表和详情'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:reimbursement:view' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '报销工作流状态重同步', 'system:workflow:reimbursement:resync',
       'MENU_ACTION', NULL, 'POST', '/system/workflow/reimbursements/resync',
       'system:workflow:reimbursement:resync', 106, 1, 1, 0, 0,
       '2026-07-20 00:00:00', 1, 'system', '2026-07-20 00:00:00', 1, 'system', 0,
       '根据流程实例补偿同步报销业务状态'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:reimbursement:resync' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流实例管理', 'system:workflow:instance:manage',
       'MENU_ACTION', NULL, NULL, '/system/workflow/instances/**', 'system:workflow:instance:manage',
       107, 1, 1, 0, 0, '2026-07-23 00:00:00', 1, 'system',
       '2026-07-23 00:00:00', 1, 'system', 0, '激活、挂起和作废工作流实例'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:instance:manage' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流任务查看', 'system:workflow:task:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/tasks/**', 'system:workflow:task:view',
       108, 1, 1, 0, 0, '2026-07-23 00:00:00', 1, 'system',
       '2026-07-23 00:00:00', 1, 'system', 0, '查看全局待办和已办任务'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:task:view' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流任务管理', 'system:workflow:task:manage',
       'MENU_ACTION', NULL, 'POST', '/system/workflow/tasks/**', 'system:workflow:task:manage',
       113, 1, 1, 0, 0, '2026-07-23 00:00:00', 1, 'system',
       '2026-07-23 00:00:00', 1, 'system', 0, '修改运行中任务办理人'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:task:manage' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流分类查看', 'system:workflow:category:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/categories/**', 'system:workflow:category:view',
       109, 1, 1, 0, 0, '2026-07-23 00:00:00', 1, 'system',
       '2026-07-23 00:00:00', 1, 'system', 0, '查看工作流分类'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:category:view' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '工作流分类管理', 'system:workflow:category:manage',
       'MENU_ACTION', NULL, NULL, '/system/workflow/categories/**', 'system:workflow:category:manage',
       110, 1, 1, 0, 0, '2026-07-23 00:00:00', 1, 'system',
       '2026-07-23 00:00:00', 1, 'system', 0, '新增、修改和安全删除工作流分类'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:category:manage' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '办理人规则查看', 'system:workflow:assignee-rule:view',
       'MENU_ACTION', NULL, 'GET', '/system/workflow/assignee-rules/**', 'system:workflow:assignee-rule:view',
       111, 1, 1, 0, 0, '2026-07-23 00:00:00', 1, 'system',
       '2026-07-23 00:00:00', 1, 'system', 0, '查看受控办理人规则'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:assignee-rule:view' AND `is_deleted` = 0
);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `menu_id`, `api_method`,
                              `api_path`, `auth_tag`, `sort_no`, `status`, `is_system`, `is_deleted`, `tenant_id`,
                              `create_time`, `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`,
                              `version`, `remark`)
SELECT @next_permission_id := @next_permission_id + 1, '办理人规则管理', 'system:workflow:assignee-rule:manage',
       'MENU_ACTION', NULL, NULL, '/system/workflow/assignee-rules/**', 'system:workflow:assignee-rule:manage',
       112, 1, 1, 0, 0, '2026-07-23 00:00:00', 1, 'system',
       '2026-07-23 00:00:00', 1, 'system', 0, '管理受控办理人规则'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:assignee-rule:manage' AND `is_deleted` = 0
);

-- ----------------------------
-- workflow role permissions
-- ----------------------------
SET @super_admin_role_id := (
    SELECT `id` FROM `sys_role`
    WHERE `role_code` = 'SUPER_ADMIN' AND `is_deleted` = 0
    LIMIT 1
);
SET @sub_admin_role_id := (
    SELECT `id` FROM `sys_role`
    WHERE `role_code` = 'SUB_ADMIN' AND `is_deleted` = 0
    LIMIT 1
);
SET @permission_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:permission:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_import_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:import' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_definition_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:definition:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_definition_manage_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:definition:manage' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_instance_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:instance:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_reimbursement_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:reimbursement:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_reimbursement_resync_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:reimbursement:resync' AND `is_deleted` = 0
    LIMIT 1
);
SET @next_role_permission_id := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_role_permission`);

-- SUPER_ADMIN: workflow import and management permissions.
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_import_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_import_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_import_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_definition_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_definition_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_definition_view_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_definition_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_definition_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_definition_manage_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_instance_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_instance_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_instance_view_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_reimbursement_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_reimbursement_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_reimbursement_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_reimbursement_resync_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_reimbursement_resync_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_reimbursement_resync_id
        AND `is_deleted` = 0
  );

-- SUB_ADMIN: permission view, workflow import and management permissions.
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @permission_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @permission_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @permission_view_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_import_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_import_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_import_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_definition_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_definition_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_definition_view_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_definition_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_definition_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_definition_manage_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_instance_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_instance_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_instance_view_id AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_reimbursement_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_reimbursement_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_reimbursement_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_reimbursement_resync_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_reimbursement_resync_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_reimbursement_resync_id
        AND `is_deleted` = 0
  );

-- 四至六阶段新增治理权限，按权限编码查询后幂等授权。
SET @workflow_instance_manage_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:instance:manage' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_task_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:task:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_task_manage_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:task:manage' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_category_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:category:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_category_manage_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:category:manage' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_assignee_rule_view_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:assignee-rule:view' AND `is_deleted` = 0
    LIMIT 1
);
SET @workflow_assignee_rule_manage_id := (
    SELECT `id` FROM `sys_permission`
    WHERE `permission_code` = 'system:workflow:assignee-rule:manage' AND `is_deleted` = 0
    LIMIT 1
);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_instance_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_instance_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_instance_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_task_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_task_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_task_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_task_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_task_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_task_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_category_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_category_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_category_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_category_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_category_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_category_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_assignee_rule_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_assignee_rule_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_assignee_rule_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @super_admin_role_id, @workflow_assignee_rule_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @super_admin_role_id IS NOT NULL AND @workflow_assignee_rule_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @super_admin_role_id AND `permission_id` = @workflow_assignee_rule_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_instance_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_instance_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_instance_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_task_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_task_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_task_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_task_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_task_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_task_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_category_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_category_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_category_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_category_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_category_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_category_manage_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_assignee_rule_view_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_assignee_rule_view_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_assignee_rule_view_id
        AND `is_deleted` = 0
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `is_deleted`, `tenant_id`, `create_time`,
                                   `create_by_id`, `create_by`, `update_time`, `update_by_id`, `update_by`, `version`)
SELECT @next_role_permission_id := @next_role_permission_id + 1,
       @sub_admin_role_id, @workflow_assignee_rule_manage_id, 0, 0, NOW(), 1, 'system', NOW(), 1, 'system', 0
WHERE @sub_admin_role_id IS NOT NULL AND @workflow_assignee_rule_manage_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission`
      WHERE `role_id` = @sub_admin_role_id AND `permission_id` = @workflow_assignee_rule_manage_id
        AND `is_deleted` = 0
  );
