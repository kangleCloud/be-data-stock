package com.vita.workflow.flow.category.service;

import com.vita.workflow.flow.category.dto.WorkflowCategoryCreateDto;
import com.vita.workflow.flow.category.dto.WorkflowCategoryDeletedDto;
import com.vita.workflow.flow.category.dto.WorkflowCategoryUpdateDto;
import com.vita.workflow.flow.category.vo.WorkflowCategoryVo;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.category.service
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流分类管理服务
 * @Version: 1.0
 */
public interface WorkflowCategoryManagementService {

    /**
     * 查询分类树。
     *
     * @return 分类树
     */
    List<WorkflowCategoryVo> tree();

    /**
     * 查询分类详情。
     *
     * @param categoryId 分类 ID
     * @return 分类详情
     */
    WorkflowCategoryVo detail(Long categoryId);

    /**
     * 创建分类。
     *
     * @param createDto 创建请求
     * @return 分类 ID
     */
    Long create(WorkflowCategoryCreateDto createDto);

    /**
     * 更新分类，分类编码保持不变。
     *
     * @param categoryId 分类 ID
     * @param updateDto 更新请求
     */
    void update(Long categoryId, WorkflowCategoryUpdateDto updateDto);

    /**
     * 删除未被子分类或流程定义引用的分类。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(WorkflowCategoryDeletedDto deletedDto);
}
