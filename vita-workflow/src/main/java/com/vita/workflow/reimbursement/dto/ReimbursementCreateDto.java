package com.vita.workflow.reimbursement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.dto
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销草稿创建参数
 * @Version: 1.0
 */
@Data
public class ReimbursementCreateDto {

    @NotBlank(message = "报销标题不能为空")
    @Size(max = 100, message = "报销标题长度不能超过100")
    private String title;

    @NotBlank(message = "费用类型不能为空")
    @Size(max = 50, message = "费用类型长度不能超过50")
    private String expenseType;

    @NotNull(message = "费用发生日期不能为空")
    private LocalDate expenseDate;

    @NotNull(message = "报销金额不能为空")
    @DecimalMin(value = "0.01", message = "报销金额必须大于0")
    @Digits(integer = 14, fraction = 2, message = "报销金额最多14位整数和2位小数")
    private BigDecimal totalAmount;

    @NotBlank(message = "报销事由不能为空")
    @Size(max = 1000, message = "报销事由长度不能超过1000")
    private String reason;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
