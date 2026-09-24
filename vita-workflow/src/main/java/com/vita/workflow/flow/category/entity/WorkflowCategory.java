package com.vita.workflow.flow.category.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流分类表。
 *
 * @author Codex
 */
@Data
@TableName("workflow_category")
@EqualsAndHashCode(callSuper = true)
public class WorkflowCategory extends BaseEntity {

    /**
     * 分类编码。
     */
    @TableField("category_code")
    private String categoryCode;

    /**
     * 分类名称。
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 父分类编码。
     */
    @TableField("parent_code")
    private String parentCode;

    /**
     * 排序。
     */
    @TableField("sort_no")
    private Integer sortNo;

    /**
     * 状态 1启用 0禁用。
     */
    @TableField("status")
    private Byte status;
}
