package com.vita.workflow.reimbursement.enums;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.enums
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销单业务状态
 * @Version: 1.0
 */
public enum ReimbursementStatus {
    DRAFT,
    APPROVING,
    RETURNED,
    APPROVED,
    CANCELLED,
    TERMINATED,
    INVALIDATED;

    /**
     * 判断是否允许申请人编辑和提交。
     *
     * @return true 允许编辑和提交
     */
    public boolean isEditable() {
        return this == DRAFT || this == RETURNED;
    }
}
