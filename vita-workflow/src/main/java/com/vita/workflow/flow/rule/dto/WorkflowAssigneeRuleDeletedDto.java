package com.vita.workflow.flow.rule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 受控办理人规则删除请求。
 *
 * @author znk
 */
@Data
public class WorkflowAssigneeRuleDeletedDto {

    /**
     * 规则 ID。
     */
    @NotNull(message = "规则ID不能为空")
    private Long id;
}
