package com.vita.workflow.constant;

import com.vita.auth.constant.AuthConstants;

import java.util.List;

/**
 * 工作流模块常量。
 *
 * @author Codex
 */
public final class WorkflowConstants {

    public static final String NODE_EXT_CAPABILITY = "VitaTaskCapability";

    public static final String NODE_EXT_COPY_USER = "VitaCopyUser";

    public static final String NODE_EXT_NEXT_ASSIGNEE = "VitaNextAssignee";

    public static final String NEXT_ASSIGNEE_FIXED = "FIXED";

    public static final String NEXT_ASSIGNEE_SELECT_USER = "SELECT_USER";

    public static final String INTERNAL_NEXT_ASSIGNEE_PREFIX = "_vitaNextAssignee:";

    /**
     * 工作流 JSON 导入权限码。
     */
    public static final String WORKFLOW_IMPORT_PERMISSION = "system:workflow:import";

    /**
     * 工作流定义查看权限码。
     */
    public static final String WORKFLOW_DEFINITION_VIEW_PERMISSION = "system:workflow:definition:view";

    /**
     * 工作流定义管理权限码。
     */
    public static final String WORKFLOW_DEFINITION_MANAGE_PERMISSION = "system:workflow:definition:manage";

    /**
     * 工作流实例查看权限码。
     */
    public static final String WORKFLOW_INSTANCE_VIEW_PERMISSION = "system:workflow:instance:view";

    /**
     * 工作流实例管理权限码。
     */
    public static final String WORKFLOW_INSTANCE_MANAGE_PERMISSION = "system:workflow:instance:manage";

    /**
     * 工作流任务查看权限码。
     */
    public static final String WORKFLOW_TASK_VIEW_PERMISSION = "system:workflow:task:view";

    /**
     * 工作流任务办理人管理权限码。
     */
    public static final String WORKFLOW_TASK_MANAGE_PERMISSION = "system:workflow:task:manage";

    /**
     * 工作流分类查看权限码。
     */
    public static final String WORKFLOW_CATEGORY_VIEW_PERMISSION = "system:workflow:category:view";

    /**
     * 工作流分类管理权限码。
     */
    public static final String WORKFLOW_CATEGORY_MANAGE_PERMISSION = "system:workflow:category:manage";

    /**
     * 工作流办理人规则查看权限码。
     */
    public static final String WORKFLOW_ASSIGNEE_RULE_VIEW_PERMISSION =
            "system:workflow:assignee-rule:view";

    /**
     * 工作流办理人规则管理权限码。
     */
    public static final String WORKFLOW_ASSIGNEE_RULE_MANAGE_PERMISSION =
            "system:workflow:assignee-rule:manage";

    /**
     * 报销工作流查看权限码。
     */
    public static final String WORKFLOW_REIMBURSEMENT_VIEW_PERMISSION = "system:workflow:reimbursement:view";

    /**
     * 报销工作流状态重同步权限码。
     */
    public static final String WORKFLOW_REIMBURSEMENT_RESYNC_PERMISSION = "system:workflow:reimbursement:resync";

    /**
     * 报销申请人办理权限变量。
     */
    public static final String APPLICANT_PERMISSION_VARIABLE = "applicantPermission";

    /**
     * 报销部门负责人办理权限变量。
     */
    public static final String DEPT_LEADER_PERMISSION_VARIABLE = "deptLeaderPermission";

    /**
     * 平台发起人变量。
     */
    public static final String INITIATOR_ID_VARIABLE = "initiatorId";

    /**
     * 平台发起人部门变量。
     */
    public static final String INITIATOR_DEPT_ID_VARIABLE = "initiatorDeptId";

    /**
     * 工作流分布式锁前缀。
     */
    public static final String WORKFLOW_LOCK_PREFIX = "vita:workflow:";

    /**
     * 客户端办理任务时不得覆盖的平台和办理人变量。
     */
    public static final List<String> PROTECTED_TASK_VARIABLES = List.of(
            INITIATOR_ID_VARIABLE,
            INITIATOR_DEPT_ID_VARIABLE,
            APPLICANT_PERMISSION_VARIABLE,
            DEPT_LEADER_PERMISSION_VARIABLE
    );

    /**
     * Warm-Flow 保存流程定义路径。
     */
    public static final String WARM_FLOW_SAVE_JSON_PATH = "/warm-flow/save-json";

    /**
     * 办理人选择器中的用户页签名称。
     */
    public static final String USER_HANDLER_TYPE = "用户";

    /**
     * 办理人选择器中的角色页签名称。
     */
    public static final String ROLE_HANDLER_TYPE = "角色";

    /**
     * 办理人选择器中的部门页签名称。
     */
    public static final String DEPT_HANDLER_TYPE = "部门";

    /**
     * 办理人选择器中的受控规则页签名称。
     */
    public static final String RULE_HANDLER_TYPE = "规则";

    /**
     * 用户权限标识前缀。
     */
    public static final String USER_PERMISSION_PREFIX = "user:";

    /**
     * 角色权限标识前缀。
     */
    public static final String ROLE_PERMISSION_PREFIX = "role:";

    /**
     * 部门权限标识前缀。
     */
    public static final String DEPT_PERMISSION_PREFIX = "dept:";

    /**
     * 节点权限标识分隔符。
     */
    public static final String PERMISSION_FLAG_SEPARATOR = "@@";

    /**
     * 人工节点类型。
     */
    public static final int USER_TASK_NODE_TYPE = 1;

    /**
     * 历史物理超管角色编码，仅用于兼容旧流程定义回显。
     */
    public static final String LEGACY_SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN_ROLE";

    /**
     * 设计器中的超级管理员显示名称。
     */
    public static final String SUPER_ADMIN_HANDLER_NAME = "超级管理员";

    /**
     * 子管理员显示名称。
     */
    public static final String SUB_ADMIN_HANDLER_NAME = "子管理员";

    /**
     * 默认补齐到人工节点的管理员角色权限标识。
     */
    public static final List<String> DEFAULT_ADMIN_ROLE_PERMISSIONS = List.of(
            buildRolePermission(AuthConstants.SUPER_ADMIN_ROLE_CODE),
            buildRolePermission(AuthConstants.SUB_ADMIN_ROLE_CODE)
    );

    private WorkflowConstants() {
        // 工具类不允许实例化
    }

    /**
     * 构建用户权限标识。
     *
     * @param userId 用户 ID
     * @return 用户权限标识
     */
    public static String buildUserPermission(Long userId) {
        return userId == null ? null : USER_PERMISSION_PREFIX + userId;
    }

    /**
     * 构建角色权限标识。
     *
     * @param roleCode 角色编码
     * @return 角色权限标识
     */
    public static String buildRolePermission(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return ROLE_PERMISSION_PREFIX + roleCode;
    }

    /**
     * 构建部门权限标识。
     *
     * @param deptId 部门 ID
     * @return 部门权限标识
     */
    public static String buildDeptPermission(Long deptId) {
        return deptId == null ? null : DEPT_PERMISSION_PREFIX + deptId;
    }
}
