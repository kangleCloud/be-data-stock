package com.vita.workflow.reimbursement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import com.vita.workflow.reimbursement.enums.ReimbursementStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.entity
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流报销单实体
 * @Version: 1.0
 */
@Data
@TableName("workflow_reimbursement")
@EqualsAndHashCode(callSuper = true)
public class WorkflowReimbursement extends BaseEntity {

    @TableField("business_id")
    private String businessId;

    @TableField("title")
    private String title;

    @TableField("expense_type")
    private String expenseType;

    @TableField("expense_date")
    private LocalDate expenseDate;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("reason")
    private String reason;

    @TableField("applicant_id")
    private Long applicantId;

    @TableField("applicant_dept_id")
    private Long applicantDeptId;

    @TableField("instance_id")
    private Long instanceId;

    @TableField("business_status")
    private ReimbursementStatus businessStatus;

    @TableField("remark")
    private String remark;
}
