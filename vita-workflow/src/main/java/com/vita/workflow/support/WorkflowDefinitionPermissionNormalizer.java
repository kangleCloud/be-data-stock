package com.vita.workflow.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.auth.constant.AuthConstants;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.constant.WorkflowConstants;
import org.dromara.warm.flow.core.dto.DefJson;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 首次创建流程时，补齐人工节点默认管理员角色。
 *
 * @author Codex
 */
@Component
@ConditionalOnWorkflowEnabled
public class WorkflowDefinitionPermissionNormalizer {

    private static final String FLOW_DEFINITION_EXISTS_SQL = "SELECT COUNT(1) FROM flow_definition WHERE id = ?";

    private static final String ROOT_ID_FIELD = "id";

    private static final String NODE_LIST_FIELD = "nodeList";

    private static final String NODE_TYPE_FIELD = "nodeType";

    private static final String PERMISSION_FLAG_FIELD = "permissionFlag";

    private final ObjectMapper objectMapper;

    private final JdbcTemplate jdbcTemplate;

    public WorkflowDefinitionPermissionNormalizer(ObjectMapper objectMapper,
                                                  JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 归一化人工节点权限配置，补齐默认管理员角色并去重。
     *
     * @param defJson 流程定义
     * @return 归一化后的流程定义
     */
    public DefJson normalizeInitialNodePermissions(DefJson defJson) {
        if (defJson == null) {
            return null;
        }

        ObjectNode root = objectMapper.valueToTree(defJson);
        JsonNode nodeList = root.get(NODE_LIST_FIELD);
        if (!(nodeList instanceof ArrayNode arrayNode)) {
            return defJson;
        }

        for (JsonNode node : arrayNode) {
            if (!(node instanceof ObjectNode nodeObject)) {
                continue;
            }
            if (nodeObject.path(NODE_TYPE_FIELD).asInt(-1) != WorkflowConstants.USER_TASK_NODE_TYPE) {
                continue;
            }
            String permissionFlag = nodeObject.path(PERMISSION_FLAG_FIELD).asText("");
            nodeObject.put(PERMISSION_FLAG_FIELD, normalizePermissionFlag(permissionFlag));
        }

        try {
            return objectMapper.treeToValue(root, DefJson.class);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), "流程定义权限归一化失败");
        }
    }

    /**
     * 判断当前保存动作是否为首次保存。
     *
     * @param defJson 流程定义
     * @return 是否首次保存
     */
    public boolean isFirstSave(DefJson defJson) {
        Long definitionId = resolveDefinitionId(defJson);
        if (definitionId == null) {
            return true;
        }
        Integer count = jdbcTemplate.queryForObject(FLOW_DEFINITION_EXISTS_SQL, Integer.class, definitionId);
        return count == null || count <= 0;
    }

    private Long resolveDefinitionId(DefJson defJson) {
        if (defJson == null) {
            return null;
        }
        JsonNode idNode = objectMapper.valueToTree(defJson).path(ROOT_ID_FIELD);
        if (idNode.isMissingNode() || idNode.isNull()) {
            return null;
        }
        if (idNode.canConvertToLong()) {
            return idNode.longValue();
        }
        String idText = idNode.asText();
        if (CharSequenceUtil.isBlank(idText)) {
            return null;
        }
        try {
            return Long.valueOf(idText.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizePermissionFlag(String permissionFlag) {
        Set<String> normalizedPermissions = new LinkedHashSet<>();
        if (CharSequenceUtil.isNotBlank(permissionFlag)) {
            for (String item : StrUtil.split(permissionFlag, WorkflowConstants.PERMISSION_FLAG_SEPARATOR)) {
                if (CharSequenceUtil.isBlank(item)) {
                    continue;
                }
                normalizedPermissions.add(canonicalizeRolePermission(item.trim()));
            }
        }
        normalizedPermissions.addAll(WorkflowConstants.DEFAULT_ADMIN_ROLE_PERMISSIONS);
        return String.join(WorkflowConstants.PERMISSION_FLAG_SEPARATOR, normalizedPermissions);
    }

    private String canonicalizeRolePermission(String permissionFlag) {
        String legacySuperAdminPermission = WorkflowConstants.buildRolePermission(WorkflowConstants.LEGACY_SUPER_ADMIN_ROLE_CODE);
        if (legacySuperAdminPermission.equals(permissionFlag)) {
            return WorkflowConstants.buildRolePermission(AuthConstants.SUPER_ADMIN_ROLE_CODE);
        }
        return permissionFlag;
    }
}
