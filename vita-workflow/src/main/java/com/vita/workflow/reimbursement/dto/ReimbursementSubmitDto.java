package com.vita.workflow.reimbursement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.dto
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销单提交参数
 * @Version: 1.0
 */
@Data
public class ReimbursementSubmitDto {

    @Size(max = 500, message = "提交意见长度不能超过500")
    private String message;

    private Map<String, Object> variables = new HashMap<>();
}
