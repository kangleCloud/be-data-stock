package com.vita.workflow.flow.category.service;

import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.flow.category.dto.WorkflowCategoryCreateDto;
import com.vita.workflow.flow.category.dto.WorkflowCategoryDeletedDto;
import com.vita.workflow.flow.category.dto.WorkflowCategoryUpdateDto;
import com.vita.workflow.flow.category.entity.WorkflowCategory;
import com.vita.workflow.flow.category.mapper.WorkflowCategoryMapper;
import com.vita.workflow.flow.category.vo.WorkflowCategoryVo;
import com.vita.workflow.runtime.mapper.WorkflowRuntimeQueryMapper;
import org.dromara.warm.flow.core.dto.Tree;
import org.dromara.warm.flow.ui.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.category.service
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: Warm-Flow 设计器与管理端共用的流程分类服务
 * @Version: 1.0
 */
@Service
@ConditionalOnWorkflowEnabled
public class WorkflowCategoryServiceImpl
        implements CategoryService, WorkflowCategoryManagementService {

    private static final Set<String> DEFAULT_CATEGORY_CODES =
            Set.of("FINANCE", "REIMBURSE", "HR", "ADMIN");

    private final WorkflowCategoryMapper workflowCategoryMapper;

    private final WorkflowRuntimeQueryMapper workflowRuntimeQueryMapper;

    public WorkflowCategoryServiceImpl(WorkflowCategoryMapper workflowCategoryMapper,
                                       WorkflowRuntimeQueryMapper workflowRuntimeQueryMapper) {
        this.workflowCategoryMapper = workflowCategoryMapper;
        this.workflowRuntimeQueryMapper = workflowRuntimeQueryMapper;
    }

    /**
     * 查询设计器使用的启用分类。
     *
     * @return 设计器分类列表
     */
    @Override
    public List<Tree> queryCategory() {
        LambdaQueryWrapperX<WorkflowCategory> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(WorkflowCategory::getStatus, CommonStatusEnum.ENABLED.getCode())
                .orderByAsc(WorkflowCategory::getSortNo, WorkflowCategory::getId);
        return workflowCategoryMapper.selectList(queryWrapper)
                .stream()
                .map(category -> new Tree(category.getCategoryCode(),
                        category.getCategoryName(),
                        category.getParentCode(),
                        null))
                .toList();
    }

    /**
     * 查询完整分类树。
     *
     * @return 分类树
     */
    @Override
    public List<WorkflowCategoryVo> tree() {
        List<WorkflowCategory> categories = workflowCategoryMapper.selectList(
                new LambdaQueryWrapperX<WorkflowCategory>()
                        .orderByAsc(WorkflowCategory::getSortNo, WorkflowCategory::getId));
        Map<String, WorkflowCategoryVo> categoryMap = new LinkedHashMap<>();
        categories.forEach(category -> categoryMap.put(
                category.getCategoryCode(), toVo(category)));
        List<WorkflowCategoryVo> roots = new ArrayList<>();
        categories.forEach(category -> {
            WorkflowCategoryVo current = categoryMap.get(category.getCategoryCode());
            WorkflowCategoryVo parent = categoryMap.get(category.getParentCode());
            if (parent == null) {
                roots.add(current);
            } else {
                parent.getChildren().add(current);
            }
        });
        return roots;
    }

    /**
     * 查询分类详情。
     *
     * @param categoryId 分类 ID
     * @return 分类详情
     */
    @Override
    public WorkflowCategoryVo detail(Long categoryId) {
        return toVo(requiredCategory(categoryId));
    }

    /**
     * 创建分类。
     *
     * @param createDto 创建请求
     * @return 分类 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkflowCategoryCreateDto createDto) {
        if (workflowCategoryMapper.selectCount(new LambdaQueryWrapperX<WorkflowCategory>()
                .eq(WorkflowCategory::getCategoryCode, createDto.getCategoryCode())) > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "分类编码已存在");
        }
        validateUniqueName(createDto.getCategoryName(), null);
        validateParent(createDto.getCategoryCode(), createDto.getParentCode());
        WorkflowCategory category = new WorkflowCategory();
        category.setCategoryCode(createDto.getCategoryCode().trim());
        category.setCategoryName(createDto.getCategoryName().trim());
        category.setParentCode(normalizeParent(createDto.getParentCode()));
        category.setSortNo(createDto.getSortNo());
        category.setStatus(createDto.getStatus());
        workflowCategoryMapper.insert(category);
        return category.getId();
    }

    /**
     * 更新分类，分类编码不允许修改。
     *
     * @param categoryId 分类 ID
     * @param updateDto 更新请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long categoryId, WorkflowCategoryUpdateDto updateDto) {
        WorkflowCategory current = requiredCategory(categoryId);
        if (DEFAULT_CATEGORY_CODES.contains(current.getCategoryCode())) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "系统默认分类不能修改");
        }
        validateUniqueName(updateDto.getCategoryName(), categoryId);
        validateParent(current.getCategoryCode(), updateDto.getParentCode());
        current.setCategoryName(updateDto.getCategoryName().trim());
        current.setParentCode(normalizeParent(updateDto.getParentCode()));
        current.setSortNo(updateDto.getSortNo());
        current.setStatus(updateDto.getStatus());
        workflowCategoryMapper.updateById(current);
    }

    /**
     * 删除未被引用的分类。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(WorkflowCategoryDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        Long categoryId = deletedDto.getId();
        WorkflowCategory category = requiredCategory(categoryId);
        if (DEFAULT_CATEGORY_CODES.contains(category.getCategoryCode())) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "系统默认分类不能删除");
        }
        long childCount = workflowCategoryMapper.selectCount(new LambdaQueryWrapperX<WorkflowCategory>()
                .eq(WorkflowCategory::getParentCode, category.getCategoryCode()));
        if (childCount > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "分类存在子分类，不能删除");
        }
        if (workflowRuntimeQueryMapper.countDefinitionsByCategory(category.getCategoryCode()) > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "分类已被流程定义引用，不能删除");
        }
        workflowCategoryMapper.deleteById(categoryId);
    }

    /**
     * 校验父分类存在且不会形成循环引用。
     *
     * @param categoryCode 当前分类编码
     * @param parentCode 父分类编码
     */
    private void validateParent(String categoryCode, String parentCode) {
        String currentParent = normalizeParent(parentCode);
        if (currentParent == null) {
            return;
        }
        if (Objects.equals(categoryCode, currentParent)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "分类不能选择自身为父分类");
        }
        int visited = 0;
        while (currentParent != null) {
            if (++visited > 100) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "分类层级存在循环");
            }
            WorkflowCategory parent = workflowCategoryMapper.selectOne(
                    new LambdaQueryWrapperX<WorkflowCategory>()
                            .eq(WorkflowCategory::getCategoryCode, currentParent));
            if (parent == null) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "父分类不存在");
            }
            if (Objects.equals(categoryCode, parent.getCategoryCode())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "分类层级存在循环");
            }
            currentParent = normalizeParent(parent.getParentCode());
        }
    }

    /**
     * 校验分类名称唯一，更新时排除当前分类。
     *
     * @param categoryName 分类名称
     * @param excludeId 更新时排除的分类 ID
     */
    private void validateUniqueName(String categoryName, Long excludeId) {
        LambdaQueryWrapperX<WorkflowCategory> query =
                new LambdaQueryWrapperX<WorkflowCategory>()
                        .eq(WorkflowCategory::getCategoryName, categoryName.trim());
        if (excludeId != null) {
            query.ne(WorkflowCategory::getId, excludeId);
        }
        if (workflowCategoryMapper.selectCount(query) > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "分类名称已存在");
        }
    }

    private WorkflowCategory requiredCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        WorkflowCategory category = workflowCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return category;
    }

    private String normalizeParent(String parentCode) {
        return parentCode == null || parentCode.isBlank() ? null : parentCode.trim();
    }

    private WorkflowCategoryVo toVo(WorkflowCategory category) {
        WorkflowCategoryVo vo = new WorkflowCategoryVo();
        vo.setId(category.getId());
        vo.setCategoryCode(category.getCategoryCode());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentCode(category.getParentCode());
        vo.setSortNo(category.getSortNo());
        vo.setStatus(category.getStatus());
        return vo;
    }
}
