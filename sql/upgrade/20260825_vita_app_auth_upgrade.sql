-- =============================================
-- File: 20260825_vita_app_auth_upgrade.sql
-- Desc: vita-app 用户认证表结构增量升级脚本
-- Warning: 本脚本只补充缺失表，不删除已有用户或 OAuth 绑定数据。
-- =============================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_user`
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

-- Step 1曾使用password_hash列；仅在旧列存在且新列不存在时原位迁移，重复执行不变更数据。
SET @app_user_password_hash_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'password_hash'
);
SET @app_user_password_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'password'
);
SET @app_user_password_migrate_sql = IF(
        @app_user_password_hash_exists > 0 AND @app_user_password_exists = 0,
        'ALTER TABLE `app_user` CHANGE COLUMN `password_hash` `password` varchar(255) NOT NULL COMMENT ''SM4加密密码''',
        'SELECT 1'
    );
PREPARE app_user_password_migrate_stmt FROM @app_user_password_migrate_sql;
EXECUTE app_user_password_migrate_stmt;
DEALLOCATE PREPARE app_user_password_migrate_stmt;

CREATE TABLE IF NOT EXISTS `app_oauth_account`
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
