package com.vita.workflow.flow.category;

import com.vita.core.exception.ServiceException;
import com.vita.workflow.flow.category.dto.WorkflowCategoryCreateDto;
import com.vita.workflow.flow.category.dto.WorkflowCategoryDeletedDto;
import com.vita.workflow.flow.category.dto.WorkflowCategoryUpdateDto;
import com.vita.workflow.flow.category.entity.WorkflowCategory;
import com.vita.workflow.flow.category.mapper.WorkflowCategoryMapper;
import com.vita.workflow.flow.category.service.WorkflowCategoryManagementService;
import com.vita.workflow.flow.category.service.WorkflowCategoryServiceImpl;
import com.vita.workflow.runtime.mapper.WorkflowRuntimeQueryMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.category
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流分类唯一性和默认分类保护测试
 * @Version: 1.0
 */
class WorkflowCategoryServiceTest {

    /**
     * 四个初始化分类禁止修改。
     */
    @Test
    void defaultCategoryShouldNotBeUpdated() {
        WorkflowCategoryManagementService service = service(category("FINANCE"), 0, 0);
        WorkflowCategoryUpdateDto updateDto = new WorkflowCategoryUpdateDto();
        updateDto.setCategoryName("财务");

        assertThatThrownBy(() -> service.update(1L, updateDto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("系统默认分类不能修改");
    }

    /**
     * 四个初始化分类禁止删除。
     */
    @Test
    void defaultCategoryShouldNotBeDeleted() {
        WorkflowCategoryManagementService service = service(category("REIMBURSE"), 0, 0);

        assertThatThrownBy(() -> service.delete(deletedDto(1L)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("系统默认分类不能删除");
    }

    @Test
    void deleteShouldRejectMissingRequestId() {
        WorkflowCategoryManagementService service = service(null, 0, 0);

        assertThatThrownBy(() -> service.delete(new WorkflowCategoryDeletedDto()))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class);
    }

    /**
     * 创建分类时除编码外还必须校验名称唯一。
     */
    @Test
    void duplicateCategoryNameShouldBeRejected() {
        WorkflowCategoryManagementService service = service(null, 0, 1);
        WorkflowCategoryCreateDto createDto = new WorkflowCategoryCreateDto();
        createDto.setCategoryCode("CUSTOM");
        createDto.setCategoryName("财务类");

        assertThatThrownBy(() -> service.create(createDto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("分类名称已存在");
    }

    private WorkflowCategoryManagementService service(
            WorkflowCategory category, long firstCount, long secondCount) {
        AtomicInteger countInvocation = new AtomicInteger();
        WorkflowCategoryMapper categoryMapper = (WorkflowCategoryMapper) Proxy.newProxyInstance(
                WorkflowCategoryMapper.class.getClassLoader(),
                new Class<?>[]{WorkflowCategoryMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> category;
                    case "selectCount" -> countInvocation.getAndIncrement() == 0
                            ? firstCount : secondCount;
                    default -> defaultValue(method.getReturnType());
                });
        WorkflowRuntimeQueryMapper queryMapper =
                (WorkflowRuntimeQueryMapper) Proxy.newProxyInstance(
                        WorkflowRuntimeQueryMapper.class.getClassLoader(),
                        new Class<?>[]{WorkflowRuntimeQueryMapper.class},
                        (proxy, method, args) -> defaultValue(method.getReturnType()));
        return new WorkflowCategoryServiceImpl(categoryMapper, queryMapper);
    }

    private WorkflowCategory category(String code) {
        WorkflowCategory category = new WorkflowCategory();
        category.setId(1L);
        category.setCategoryCode(code);
        category.setCategoryName(code);
        return category;
    }

    private WorkflowCategoryDeletedDto deletedDto(Long id) {
        WorkflowCategoryDeletedDto deletedDto = new WorkflowCategoryDeletedDto();
        deletedDto.setId(id);
        return deletedDto;
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == boolean.class) {
            return false;
        }
        return null;
    }
}
