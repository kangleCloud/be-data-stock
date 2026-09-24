package com.vita.common.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vita.system.sysMenu.entity.SysMenu;
import lombok.Data;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.common.core.model
 * @Author: znk
 * @CreateTime: 2026-04-06  19:09:16
 * @Description: 树形下拉选择框模型
 * @Version: 1.0
 */
@Data
public class TreeSelectModel {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    private Long id;

    /**
     * 节点名称
     */
    private String label;

    /**
     * 节点禁用
     */
    private boolean disabled = false;

    /**
     * 子节点
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<TreeSelectModel> children;

    public TreeSelectModel() {
        // 默认构造函数
    }

    /**
     * 构造函数，根据 SysMenu 实例创建 TreeSelectModel 对象。
     *
     * @param sysMenu 目录对象
     */
    public TreeSelectModel(SysMenu sysMenu) {
        this.id = sysMenu.getId();
        this.label = sysMenu.getMenuName();
        this.children = sysMenu.getChildren().stream().map(TreeSelectModel::new).toList();
    }
}
