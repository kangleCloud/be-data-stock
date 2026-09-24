package com.vita.workflow.runtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.entity
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流实例业务摘要元数据
 * @Version: 1.0
 */
@Data
@TableName("workflow_instance_metadata")
@EqualsAndHashCode(callSuper = true)
public class WorkflowInstanceMetadata extends BaseEntity {

    private String businessId;

    private Long instanceId;

    private String flowCode;

    private String businessCode;

    private String businessTitle;
}
