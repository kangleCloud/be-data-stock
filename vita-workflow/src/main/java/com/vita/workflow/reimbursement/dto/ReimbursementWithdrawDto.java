package com.vita.workflow.reimbursement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.dto
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销单撤回参数
 * @Version: 1.0
 */
@Data
public class ReimbursementWithdrawDto {

    @Size(max = 500, message = "撤回原因长度不能超过500")
    private String message;
}
