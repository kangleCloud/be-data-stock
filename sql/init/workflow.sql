CREATE TABLE `flow_definition`
(
    `id`              bigint          NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)     NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100)    NOT NULL COMMENT '流程名称',
    `model_value`            varchar(40)     NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
    `category`        varchar(100)             DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)     NOT NULL COMMENT '流程版本',
    `is_publish`      tinyint(1)      NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_custom`     char(1)                  DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)             DEFAULT NULL COMMENT '审批表单路径',
    `activity_status` tinyint(1)      NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   varchar(100)             DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)             DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)             DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`     datetime                 DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime                 DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`        char(1)                  DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)              DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程定义表';

CREATE TABLE `flow_node`
(
    `id`              bigint        NOT NULL COMMENT '主键id',
    `node_type`       tinyint(1)      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `definition_id`   bigint          NOT NULL COMMENT '流程定义id',
    `node_code`       varchar(100)    NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)  DEFAULT NULL COMMENT '流程节点名称',
    `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
    `node_ratio`      varchar(200) DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
    `form_custom`     char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
    `version`         varchar(20)     NOT NULL COMMENT '版本',
    `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             text          COMMENT '节点扩展属性',
    `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程节点表';

CREATE TABLE `flow_skip`
(
    `id`             bigint       NOT NULL COMMENT '主键id',
    `definition_id`  bigint          NOT NULL COMMENT '流程定义id',
    `now_node_code`  varchar(100)    NOT NULL COMMENT '当前流程节点的编码',
    `now_node_type`  tinyint(1)   DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `next_node_code` varchar(100)    NOT NULL COMMENT '下一个流程节点的编码',
    `next_node_type` tinyint(1)   DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `skip_name`      varchar(100) DEFAULT NULL COMMENT '跳转名称',
    `skip_type`      varchar(40)  DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
    `skip_condition` varchar(200) DEFAULT NULL COMMENT '跳转条件',
    `coordinate`     varchar(100) DEFAULT NULL COMMENT '坐标',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`       char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`      varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='节点跳转关联表';

CREATE TABLE `flow_instance`
(
    `id`              bigint      NOT NULL COMMENT '主键id',
    `definition_id`   bigint      NOT NULL COMMENT '对应flow_definition表的id',
    `business_id`     varchar(40) NOT NULL COMMENT '业务id',
    `node_type`       tinyint(1)  NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `node_code`       varchar(40) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variable`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        text COMMENT '流程定义json',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             varchar(500)         DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
    `del_flag`        char(1)              DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)          DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程实例表';

CREATE TABLE `flow_task`
(
    `id`            bigint       NOT NULL COMMENT '主键id',
    `definition_id` bigint       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`   bigint       NOT NULL COMMENT '对应flow_instance表的id',
    `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
    `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
    `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`   char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`     varchar(100) DEFAULT NULL COMMENT '审批表单路径',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='待办任务表';

CREATE TABLE `flow_his_task`
(
    `id`               bigint(20)                   NOT NULL COMMENT '主键id',
    `definition_id`    bigint(20)                   NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`      bigint(20)                   NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`          bigint(20)                   NOT NULL COMMENT '对应flow_task表的id',
    `node_code`        varchar(100)                 DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100)                 DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)                   DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `target_node_code` varchar(200)                 DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200)                 DEFAULT NULL COMMENT '结束节点名称',
    `approver`         varchar(40)                  DEFAULT NULL COMMENT '审批人',
    `cooperate_type`   tinyint(1)                   NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
    `collaborator`     varchar(500)                  DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)                  NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`      varchar(20)                  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`      char(1)                      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`        varchar(100)                 DEFAULT NULL COMMENT '审批表单路径',
    `message`          varchar(500)                 DEFAULT NULL COMMENT '审批意见',
    `variable`         TEXT                         DEFAULT NULL COMMENT '任务变量',
    `ext`              TEXT                         DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`      datetime                     DEFAULT NULL COMMENT '任务开始时间',
    `update_time`      datetime                     DEFAULT NULL COMMENT '审批完成时间',
    `del_flag`         char(1)                      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)                  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='历史任务记录表';


CREATE TABLE `flow_user`
(
    `id`           bigint      NOT NULL COMMENT '主键id',
    `type`         char(1)         NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `associated`   bigint          NOT NULL COMMENT '任务表id',
    `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `user_processed_type` (`processed_by`, `type`),
    KEY `user_associated` (`associated`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程用户表';

CREATE TABLE `flow_form`
(
    `id`           bigint(20) UNSIGNED NOT NULL COMMENT '主键id',
    `form_code`    varchar(40)         NOT NULL COMMENT '表单编码',
    `form_name`    varchar(100)        NOT NULL COMMENT '表单名称',
    `version`      varchar(20)         NOT NULL COMMENT '表单版本',
    `is_publish`   tinyint(1)          NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_type`    tinyint(1)                   DEFAULT '0' COMMENT '表单类型（0内置表单 存form_content 1外挂表单 存form_path）',
    `form_path`    varchar(100)                  DEFAULT NULL COMMENT '表单路径',
    `form_content` longtext                      DEFAULT NULL COMMENT '表单内容',
    `ext`          varchar(400)                  DEFAULT NULL COMMENT '表单扩展，用户自行使用',
    `create_time`  datetime                      DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(64)                   DEFAULT '' COMMENT '创建人',
    `update_time`  datetime                      DEFAULT NULL COMMENT '更新时间',
    `update_by`    varchar(64)                   DEFAULT '' COMMENT '更新人',
    `del_flag`     char(1)                       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40)                   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程表单表';

-- ----------------------------
-- be-vita workflow extension tables
-- ----------------------------
DROP TABLE IF EXISTS `workflow_category`;
CREATE TABLE `workflow_category`
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

DROP TABLE IF EXISTS `workflow_reimbursement`;
CREATE TABLE `workflow_reimbursement`
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
