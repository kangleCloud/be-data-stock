/*
 Navicat Premium Dump SQL

 Source Server         : kangCloud
 Source Server Type    : MySQL
 Source Server Version : 80037 (8.0.37)
 Source Host           : 106.54.205.33:3306
 Source Schema         : data_stock

 Target Server Type    : MySQL
 Target Server Version : 80037 (8.0.37)
 File Encoding         : 65001

 Date: 20/03/2026 16:09:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`
(
    `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module`          varchar(100) DEFAULT NULL COMMENT '模块',
    `business_type`   varchar(100) DEFAULT NULL COMMENT '业务类型',
    `request_method`  varchar(20)  DEFAULT NULL COMMENT '请求方式',
    `oper_url`        varchar(500) DEFAULT NULL COMMENT '请求URL',
    `oper_ip`         varchar(64)  DEFAULT NULL COMMENT '操作IP',
    `oper_location`   varchar(255) DEFAULT NULL COMMENT '操作地点',
    `oper_name`       varchar(100) DEFAULT NULL COMMENT '操作人名称',
    `oper_user_id`    bigint       DEFAULT NULL COMMENT '操作人ID',
    `class_name`      varchar(255) DEFAULT NULL COMMENT '类名',
    `method_name`     varchar(255) DEFAULT NULL COMMENT '方法名',
    `request_param`   longtext COMMENT '请求参数',
    `response_result` longtext COMMENT '返回结果',
    `status`          tinyint      DEFAULT '1' COMMENT '状态 1成功 0失败',
    `error_msg`       longtext COMMENT '错误信息',
    `cost_time`       bigint       DEFAULT '0' COMMENT '耗时(ms)',
    `oper_time`       datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `create_by_id`    bigint       DEFAULT NULL COMMENT '创建人ID',
    `create_by`       varchar(64)  DEFAULT NULL COMMENT '创建人',
    `create_time`     datetime NOT NULL COMMENT '创建时间',
    `update_by_id`    bigint       DEFAULT NULL COMMENT '更新人ID',
    `update_by`       varchar(64)  DEFAULT NULL COMMENT '更新人',
    `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
    `tenant_id`       bigint       DEFAULT '0' COMMENT '租户ID',
    `version`         int          DEFAULT '0' COMMENT '版本号',
    `is_deleted`      tinyint      DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统操作日志表';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              bigint                                                        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`       bigint                                                                 DEFAULT NULL COMMENT '租户ID',
    `dept_id`         bigint                                                                 DEFAULT NULL COMMENT '部门ID',
    `nick_name`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '昵称',
    `user_name`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '用户名称',
    `password`        varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
    `mobile`          varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '手机号',
    `email`           varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '邮箱',
    `avatar_url`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '头像',
    `gender`          char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci               DEFAULT '2' COMMENT '性别(0-女,1-男,2-其他)',
    `status`          tinyint                                                       NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
    `is_super_admin`  tinyint                                                       NOT NULL DEFAULT '0' COMMENT '是否超级管理员 1是 0否',
    `is_system`       tinyint                                                       NOT NULL DEFAULT '0' COMMENT '是否系统内置 1是 0否',
    `login_time`      datetime                                                               DEFAULT NULL COMMENT '上次登录时间',
    `login_address`   varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '登录地址',
    `pwd_update_date` datetime                                                               DEFAULT NULL COMMENT '密码最后更新时间',
    `create_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`     datetime                                                               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1)                                                              DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`    bigint                                                                 DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`    bigint                                                                 DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                                                                 DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name` (`user_name`),
    KEY               `idx_user_user_name` (`user_name`),
    KEY               `idx_user_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息表';

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`
(
    `id`             bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
    `user_name`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '用户账号',
    `ipaddr`         varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录IP地址',
    `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录地点',
    `browser`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '浏览器类型',
    `os`             varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT '' COMMENT '操作系统',
    `status`         char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci      DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
    `msg`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '提示消息',
    `login_time`     datetime                                                      DEFAULT NULL COMMENT '访问时间',
    `create_time`    datetime                                                      DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`    datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     tinyint(1) DEFAULT '0' COMMENT '是否删除',
    `tenant_id`      bigint                                                        DEFAULT NULL COMMENT '租户ID',
    `create_by_id`   bigint                                                        DEFAULT NULL COMMENT '新增用户ID',
    `create_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`   bigint                                                        DEFAULT NULL COMMENT '更新用户ID',
    `update_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '更新用户名称',
    `version`        bigint                                                        DEFAULT '0' COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    KEY              `idx_sys_logininfor_s` (`status`),
    KEY              `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统访问记录';

-- ----------------------------
-- Table structure for sys_ip_block
-- ----------------------------
DROP TABLE IF EXISTS `sys_ip_block`;
CREATE TABLE `sys_ip_block`
(
    `id`           bigint                                 NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `ip`           varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'IP地址',
    `status`       tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
    `start_time`   datetime                               NOT NULL COMMENT '开始时间',
    `end_time`     datetime                                DEFAULT NULL COMMENT '结束时间，永久封禁为空',
    `tenant_id`    bigint                                  DEFAULT NULL COMMENT '租户ID',
    `remark`       varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    `create_by_id` bigint                                  DEFAULT NULL COMMENT '创建人ID',
    `create_by`    varchar(64) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '创建人',
    `create_time`  datetime                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by_id` bigint                                  DEFAULT NULL COMMENT '更新人ID',
    `update_by`    varchar(64) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '更新人',
    `update_time`  datetime                                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`      int                                     DEFAULT '0' COMMENT '版本号',
    `is_deleted`   tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY            `idx_ip_status` (`ip`,`status`),
    KEY            `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IP封禁表';

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`
(
    `id`              bigint      NOT NULL COMMENT '角色ID',
    `role_name`       varchar(64) NOT NULL COMMENT '角色名称',
    `role_code`       varchar(64) NOT NULL COMMENT '角色编码',
    `role_sort`       int         NOT NULL                                          DEFAULT 0 COMMENT '显示顺序',
    `status`          tinyint     NOT NULL                                          DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `data_scope_type` varchar(32) NOT NULL                                          DEFAULT 'SELF' COMMENT '数据范围 ALL/DEPT_AND_CHILD/DEPT_SELF/SELF/CUSTOM',
    `is_system`       tinyint     NOT NULL                                          DEFAULT 0 COMMENT '是否系统内置 1是 0否',
    `is_deleted`      tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`       bigint                                                        DEFAULT NULL COMMENT '租户ID',
    `create_time`     datetime    NOT NULL                                          DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`    bigint                                                        DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '新增用户名称',
    `update_time`     datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`    bigint                                                        DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                                                        DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`, `is_deleted`),
    KEY               `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`
(
    `id`             bigint      NOT NULL COMMENT '菜单ID',
    `parent_id`      bigint      NOT NULL                                          DEFAULT 0 COMMENT '父菜单ID',
    `menu_name`      varchar(64) NOT NULL COMMENT '菜单名称',
    `menu_type`      varchar(16) NOT NULL COMMENT '类型 CONTENTS/MENU/LINK',
    `route_name`     varchar(64)                                                   DEFAULT NULL COMMENT '路由名称',
    `route_link`     varchar(200)                                                  DEFAULT NULL COMMENT '路由地址',
    `component_path` varchar(255)                                                  DEFAULT NULL COMMENT '组件路径',
    `icon`           varchar(100)                                                  DEFAULT NULL COMMENT '图标',
    `sort_no`        int         NOT NULL                                          DEFAULT 0 COMMENT '排序',
    `visible`        tinyint     NOT NULL                                          DEFAULT 1 COMMENT '是否显示 1是 0否',
    `is_cache`       tinyint     NOT NULL                                          DEFAULT 0 COMMENT '是否缓存 1是 0否',
    `always_show`    tinyint     NOT NULL                                          DEFAULT 0 COMMENT '是否总显示',
    `is_external`    tinyint     NOT NULL                                          DEFAULT 0 COMMENT '是否外链',
    `status`         tinyint     NOT NULL                                          DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `is_system`      tinyint     NOT NULL                                          DEFAULT 0 COMMENT '是否系统内置 1是 0否',
    `is_deleted`     tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`      bigint                                                        DEFAULT NULL COMMENT '租户ID',
    `create_time`    datetime    NOT NULL                                          DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`   bigint                                                        DEFAULT NULL COMMENT '新增用户ID',
    `create_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '新增用户名称',
    `update_time`    datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`   bigint                                                        DEFAULT NULL COMMENT '更新用户ID',
    `update_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '更新用户名称',
    `version`        bigint                                                        DEFAULT '0' COMMENT '乐观锁',
    `remark`         varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY              `idx_parent_id` (`parent_id`),
    KEY              `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`
(
    `id`              bigint       NOT NULL COMMENT '权限ID',
    `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
    `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
    `permission_type` varchar(32)  NOT NULL COMMENT '权限类型 MENU_ACTION/API/DATA/FIELD',
    `menu_id`         bigint                                                        DEFAULT NULL COMMENT '关联菜单ID，可为空',
    `api_method`      varchar(16)                                                   DEFAULT NULL COMMENT '接口方法 GET/POST',
    `api_path`        varchar(255)                                                  DEFAULT NULL COMMENT '接口路径',
    `auth_tag`        varchar(100)                                                  DEFAULT NULL COMMENT 'Sa-Token鉴权标识，通常等于permission_code',
    `sort_no`         int          NOT NULL                                         DEFAULT 0 COMMENT '排序',
    `status`          tinyint      NOT NULL                                         DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `is_system`       tinyint      NOT NULL                                         DEFAULT 0 COMMENT '是否系统内置 1是 0否',
    `is_deleted`      tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`       bigint                                                        DEFAULT NULL COMMENT '租户ID',
    `create_time`     datetime     NOT NULL                                         DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`    bigint                                                        DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '新增用户名称',
    `update_time`     datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`    bigint                                                        DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                                                        DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`, `is_deleted`),
    KEY               `idx_menu_id` (`menu_id`),
    KEY               `idx_permission_type` (`permission_type`),
    KEY               `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`
(
    `id`             bigint      NOT NULL COMMENT '部门ID',
    `parent_id`      bigint      NOT NULL                                          DEFAULT 0 COMMENT '父部门ID',
    `dept_name`      varchar(64) NOT NULL COMMENT '部门名称',
    `dept_code`      varchar(64)                                                   DEFAULT NULL COMMENT '部门编码',
    `ancestors`      varchar(500)                                                  DEFAULT NULL COMMENT '祖级路径，如 0,100,101',
    `leader_user_id` bigint                                                        DEFAULT NULL COMMENT '负责人ID',
    `phone`          varchar(20)                                                   DEFAULT NULL COMMENT '联系电话',
    `email`          varchar(128)                                                  DEFAULT NULL COMMENT '邮箱',
    `sort_no`        int         NOT NULL                                          DEFAULT 0 COMMENT '排序',
    `status`         tinyint     NOT NULL                                          DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `is_system`      tinyint     NOT NULL                                          DEFAULT 0 COMMENT '是否系统内置 1是 0否',
    `is_deleted`     tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`      bigint                                                        DEFAULT NULL COMMENT '租户ID',
    `create_time`    datetime    NOT NULL                                          DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`   bigint                                                        DEFAULT NULL COMMENT '新增用户ID',
    `create_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '新增用户名称',
    `update_time`    datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`   bigint                                                        DEFAULT NULL COMMENT '更新用户ID',
    `update_by`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '更新用户名称',
    `version`        bigint                                                        DEFAULT '0' COMMENT '乐观锁',
    `remark`         varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY              `idx_parent_id` (`parent_id`),
    KEY              `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`
(
    `id`           bigint   NOT NULL COMMENT '主键ID',
    `user_id`      bigint   NOT NULL COMMENT '用户ID',
    `role_id`      bigint   NOT NULL COMMENT '角色ID',
    `is_deleted`   tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`    bigint                                                       DEFAULT NULL COMMENT '租户ID',
    `create_time`  datetime NOT NULL                                            DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id` bigint                                                       DEFAULT NULL COMMENT '新增用户ID',
    `create_by`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新增用户名称',
    `update_time`  datetime                                                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id` bigint                                                       DEFAULT NULL COMMENT '更新用户ID',
    `update_by`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新用户名称',
    `version`      bigint                                                       DEFAULT '0' COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`,`is_deleted`),
    KEY            `idx_user_id` (`user_id`),
    KEY            `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`
(
    `id`           bigint   NOT NULL COMMENT '主键ID',
    `role_id`      bigint   NOT NULL COMMENT '角色ID',
    `menu_id`      bigint   NOT NULL COMMENT '菜单ID',
    `is_deleted`   tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`    bigint                                                       DEFAULT NULL COMMENT '租户ID',
    `create_time`  datetime NOT NULL                                            DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id` bigint                                                       DEFAULT NULL COMMENT '新增用户ID',
    `create_by`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新增用户名称',
    `update_time`  datetime                                                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id` bigint                                                       DEFAULT NULL COMMENT '更新用户ID',
    `update_by`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新用户名称',
    `version`      bigint                                                       DEFAULT '0' COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` ( `role_id`, `menu_id`,`is_deleted`),
    KEY            `idx_role_id` (`role_id`),
    KEY            `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`
(
    `id`            bigint   NOT NULL COMMENT '主键ID',
    `role_id`       bigint   NOT NULL COMMENT '角色ID',
    `permission_id` bigint   NOT NULL COMMENT '权限ID',
    `is_deleted`    tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`     bigint                                                       DEFAULT NULL COMMENT '租户ID',
    `create_time`   datetime NOT NULL                                            DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id`  bigint                                                       DEFAULT NULL COMMENT '新增用户ID',
    `create_by`     varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新增用户名称',
    `update_time`   datetime                                                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id`  bigint                                                       DEFAULT NULL COMMENT '更新用户ID',
    `update_by`     varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新用户名称',
    `version`       bigint                                                       DEFAULT '0' COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`,`is_deleted`),
    KEY             `idx_role_id` (`role_id`),
    KEY             `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ----------------------------
-- Table structure for sys_role_dept_scope
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept_scope`;
CREATE TABLE `sys_role_dept_scope`
(
    `id`           bigint   NOT NULL COMMENT '主键ID',
    `role_id`      bigint   NOT NULL COMMENT '角色ID',
    `dept_id`      bigint   NOT NULL COMMENT '部门ID',
    `is_deleted`   tinyint(1) DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `tenant_id`    bigint                                                       DEFAULT NULL COMMENT '租户ID',
    `create_time`  datetime NOT NULL                                            DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `create_by_id` bigint                                                       DEFAULT NULL COMMENT '新增用户ID',
    `create_by`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新增用户名称',
    `update_time`  datetime                                                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by_id` bigint                                                       DEFAULT NULL COMMENT '更新用户ID',
    `update_by`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新用户名称',
    `version`      bigint                                                       DEFAULT '0' COMMENT '乐观锁',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_dept_scope` (`role_id`, `dept_id`,`is_deleted`),
    KEY            `idx_role_id` (`role_id`),
    KEY            `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色自定义数据范围表';
