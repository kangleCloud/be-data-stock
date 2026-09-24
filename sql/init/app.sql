-- =============================================
-- File: app.sql
-- Desc: vita-app 用户认证初始化表结构
-- Warning: 本脚本会删除并重建 app 用户与 OAuth 绑定表，已有数据将永久丢失。
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- OAuth 绑定表依赖 app 用户语义，重建时先删除绑定表。
DROP TABLE IF EXISTS `app_oauth_account`;
DROP TABLE IF EXISTS `app_user`;

CREATE TABLE `app_user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `tenant_id`       bigint                DEFAULT NULL COMMENT '租户ID',
    `user_name`       varchar(32)  NOT NULL COMMENT '用户名称',
    `password`        varchar(255) NOT NULL COMMENT 'SM4加密密码',
    `nick_name`       varchar(64)  NOT NULL COMMENT '用户昵称',
    `avatar_url`      varchar(500)          DEFAULT NULL COMMENT '头像地址',
    `status`          tinyint      NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
    `login_time`      datetime              DEFAULT NULL COMMENT '最后登录时间',
    `login_address`   varchar(128)          DEFAULT NULL COMMENT '最后登录地址',
    `pwd_update_date` datetime     NOT NULL COMMENT '密码最后更新时间',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`     datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint(1)            DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`    bigint                DEFAULT NULL COMMENT '新增用户ID',
    `create_by`       varchar(50)           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`    bigint                DEFAULT NULL COMMENT '更新用户ID',
    `update_by`       varchar(50)           DEFAULT NULL COMMENT '更新用户名称',
    `version`         bigint                DEFAULT '0' COMMENT '乐观锁',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user_name` (`user_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='App用户表';

CREATE TABLE `app_oauth_account`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT 'OAuth绑定ID',
    `tenant_id`          bigint                DEFAULT NULL COMMENT '租户ID',
    `user_id`            bigint       NOT NULL COMMENT 'App用户ID',
    `provider_code`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'OAuth平台编码',
    `provider_user_id`   varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'OAuth平台用户ID',
    `provider_login`     varchar(128)          DEFAULT NULL COMMENT '平台登录名快照',
    `provider_nick_name` varchar(100)          DEFAULT NULL COMMENT '平台昵称快照',
    `avatar_url`         varchar(500)          DEFAULT NULL COMMENT '平台头像快照',
    `profile_url`        varchar(500)          DEFAULT NULL COMMENT '平台公开主页',
    `bind_time`          datetime     NOT NULL COMMENT '首次绑定时间',
    `last_auth_time`     datetime              DEFAULT NULL COMMENT '最近OAuth认证时间',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
    `update_time`        datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         tinyint(1)            DEFAULT '0' COMMENT '是否删除(0-否,1-是)',
    `create_by_id`       bigint                DEFAULT NULL COMMENT '新增用户ID',
    `create_by`          varchar(50)           DEFAULT NULL COMMENT '新增用户名称',
    `update_by_id`       bigint                DEFAULT NULL COMMENT '更新用户ID',
    `update_by`          varchar(50)           DEFAULT NULL COMMENT '更新用户名称',
    `version`            bigint                DEFAULT '0' COMMENT '乐观锁',
    `remark`             varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_oauth_provider_user` (`provider_code`, `provider_user_id`),
    UNIQUE KEY `uk_app_oauth_user_provider` (`user_id`, `provider_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='App OAuth账号绑定表';

SET FOREIGN_KEY_CHECKS = 1;
