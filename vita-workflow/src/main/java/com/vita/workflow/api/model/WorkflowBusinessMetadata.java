package com.vita.workflow.api.model;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.api.model
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流业务摘要元数据
 * @Version: 1.0
 */
@Data
public class WorkflowBusinessMetadata {

    @Size(max = 100, message = "业务编码长度不能超过100")
    private String businessCode;

    @Size(max = 500, message = "业务标题长度不能超过500")
    private String businessTitle;
}
