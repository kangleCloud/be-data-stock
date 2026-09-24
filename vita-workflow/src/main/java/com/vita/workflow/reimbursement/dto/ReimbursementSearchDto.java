package com.vita.workflow.reimbursement.dto;

import com.vita.core.page.PageRequest;
import com.vita.workflow.reimbursement.enums.ReimbursementStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.dto
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销单分页查询参数
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReimbursementSearchDto extends PageRequest {

    private ReimbursementStatus businessStatus;
    private Long applicantId;
    private String keyword;
}
