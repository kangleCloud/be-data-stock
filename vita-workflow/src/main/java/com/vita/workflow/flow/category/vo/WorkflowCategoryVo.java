package com.vita.workflow.flow.category.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.category.vo
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流分类视图
 * @Version: 1.0
 */
@Data
public class WorkflowCategoryVo {

    private Long id;

    private String categoryCode;

    private String categoryName;

    private String parentCode;

    private Integer sortNo;

    private Byte status;

    private List<WorkflowCategoryVo> children = new ArrayList<>();
}
