package com.vita.workflow.runtime.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vita.workflow.runtime.dto.WorkflowDefinitionSearchDto;
import com.vita.workflow.runtime.dto.WorkflowInstanceSearchDto;
import com.vita.workflow.runtime.dto.WorkflowTaskSearchDto;
import com.vita.workflow.runtime.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.mapper
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: Warm-Flow 引擎表专用只读关联查询 Mapper
 * @Version: 1.0
 */
@Mapper
public interface WorkflowRuntimeQueryMapper {

    IPage<WorkflowDefinitionVo> selectDefinitionPage(Page<WorkflowDefinitionVo> page,
                                                      @Param("query") WorkflowDefinitionSearchDto query,
                                                      @Param("publishedOnly") boolean publishedOnly);

    WorkflowDefinitionVo selectDefinitionById(@Param("definitionId") Long definitionId);

    IPage<WorkflowInstanceVo> selectInstancePage(Page<WorkflowInstanceVo> page,
                                                  @Param("query") WorkflowInstanceSearchDto query,
                                                  @Param("initiatorId") String initiatorId,
                                                  @Param("finished") Boolean finished);

    WorkflowInstanceVo selectInstanceById(@Param("instanceId") Long instanceId);

    WorkflowInstanceVo selectInstanceByBusinessId(@Param("businessId") String businessId);

    IPage<WorkflowTaskVo> selectPendingTaskPage(Page<WorkflowTaskVo> page,
                                                @Param("query") WorkflowTaskSearchDto query,
                                                @Param("permissions") List<String> permissions);

    IPage<WorkflowTaskVo> selectCompletedTaskPage(Page<WorkflowTaskVo> page,
                                                  @Param("query") WorkflowTaskSearchDto query,
                                                  @Param("approverId") String approverId);

    IPage<WorkflowTaskVo> selectAllPendingTaskPage(Page<WorkflowTaskVo> page,
                                                   @Param("query") WorkflowTaskSearchDto query);

    IPage<WorkflowTaskVo> selectAllCompletedTaskPage(Page<WorkflowTaskVo> page,
                                                     @Param("query") WorkflowTaskSearchDto query);

    IPage<WorkflowCopyVo> selectCopiedTaskPage(Page<WorkflowCopyVo> page,
                                               @Param("query") WorkflowTaskSearchDto query,
                                               @Param("recipientUserId") Long recipientUserId);

    WorkflowTaskVo selectTaskById(@Param("taskId") Long taskId);

    List<WorkflowHistoryVo> selectHistoryByInstanceId(@Param("instanceId") Long instanceId);

    long countInstanceAccess(@Param("instanceId") Long instanceId,
                             @Param("userId") String userId,
                             @Param("permissions") List<String> permissions);

    long countInstanceAccessByBusinessId(@Param("businessId") String businessId,
                                         @Param("userId") String userId,
                                         @Param("permissions") List<String> permissions);

    long countTaskAccess(@Param("taskId") Long taskId,
                         @Param("permissions") List<String> permissions);

    long countInstancesByDefinitionId(@Param("definitionId") Long definitionId);

    long countDefinitionsByCategory(@Param("categoryCode") String categoryCode);

    long countNodesByPermissionExpression(@Param("expression") String expression);

    Long selectCurrentTaskId(@Param("instanceId") Long instanceId);

    Long selectLatestHistoryIdByTaskId(@Param("taskId") Long taskId);
}
