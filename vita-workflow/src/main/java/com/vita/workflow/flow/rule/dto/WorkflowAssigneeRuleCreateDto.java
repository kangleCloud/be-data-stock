package com.vita.workflow.flow.rule.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 受控办理人规则创建请求
 * @Version: 1.0
 */
@Data
public class WorkflowAssigneeRuleCreateDto {

    @NotBlank(message = "规则编码不能为空")
    @Size(max = 64, message = "规则编码长度不能超过64")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "规则编码只能使用大写字母、数字和下划线")
    private String ruleCode;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过100")
    private String ruleName;

    @NotBlank(message = "解析器编码不能为空")
    @Size(max = 64, message = "解析器编码长度不能超过64")
    private String resolverCode;

    @Size(max = 10, message = "规则参数数量不能超过10")
    private List<@Pattern(
            regexp = "^[A-Za-z][A-Za-z0-9_]*$",
            message = "规则参数名只能使用字母、数字和下划线") String> argumentNames = new ArrayList<>();

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Byte status;
}
