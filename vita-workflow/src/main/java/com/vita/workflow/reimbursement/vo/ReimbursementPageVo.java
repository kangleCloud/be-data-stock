package com.vita.workflow.reimbursement.vo;

import com.vita.workflow.reimbursement.enums.ReimbursementStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.vo
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销单分页展示对象
 * @Version: 1.0
 */
@Data
public class ReimbursementPageVo {

    private Long id;
    private String businessId;
    private String title;
    private String expenseType;
    private LocalDate expenseDate;
    private BigDecimal totalAmount;
    private Long applicantId;
    private Long applicantDeptId;
    private Long instanceId;
    private ReimbursementStatus businessStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
