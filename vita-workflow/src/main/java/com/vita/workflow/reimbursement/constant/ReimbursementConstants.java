package com.vita.workflow.reimbursement.constant;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.constant
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销工作流常量
 * @Version: 1.0
 */
public final class ReimbursementConstants {

    public static final String FLOW_CODE = "REIMBURSE_APPROVAL_V1";
    public static final String BUSINESS_CODE = "REIMBURSEMENT";
    public static final String BUSINESS_ID_PREFIX = "REIMBURSE:";
    public static final String APPLICANT_NODE_CODE = "submit_apply";
    public static final String END_NODE_CODE = "end";

    private ReimbursementConstants() {
    }

    /**
     * 构建全局业务 ID。
     *
     * @param reimbursementId 报销单 ID
     * @return 全局业务 ID
     */
    public static String buildBusinessId(Long reimbursementId) {
        return BUSINESS_ID_PREFIX + reimbursementId;
    }
}
