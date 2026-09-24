package com.vita.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.entity
 * @Author: znk
 * @CreateTime: 2026-03-04  14:16:52
 * @Description: 基础实体类，包含通用字段和逻辑删除标记
 * <p>
 * 该类用于所有实体类的基类，提供了通用的字段如创建时间、更新时间、创建人、更新人等。
 * 还包含逻辑删除字段和租户ID字段，适用于多租户场景。
 * </p>
 * @Version: 1.0
 */
@Getter
@Setter
public class BaseEntity implements Serializable {

    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createById;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateById;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 逻辑删除字段，使用 MyBatis-Plus 的 @TableLogic 注解标记。
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic
    private Boolean isDeleted;

    private Long tenantId;

    private int version;
}
