package com.vita.workflow.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.dto
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流任务办理人修改请求
 * @Version: 1.0
 */
@Data
public class WorkflowTaskHandlersUpdateRequest {

    @Valid
    @NotEmpty(message = "办理人不能为空")
    @Size(min = 1, max = 20, message = "办理人数量必须在1到20之间")
    private List<@NotNull(message = "办理人ID不能为空") Long> userIds;

    @NotBlank(message = "修改原因不能为空")
    @Size(max = 500, message = "修改原因长度不能超过500")
    private String reason;
}
