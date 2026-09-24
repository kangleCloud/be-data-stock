package com.vita.workflow.runtime.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.workflow.runtime.entity.WorkflowTaskCopy;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.mapper
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流抄送记录 Mapper
 * @Version: 1.0
 */
@Mapper
public interface WorkflowTaskCopyMapper extends BaseMapperX<WorkflowTaskCopy> {
}
