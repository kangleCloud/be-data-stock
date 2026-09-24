package com.vita.system.sysRole.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.dto
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 创建 DTO
 * @version 1.0
 */
@Data
public class SysRoleCreateDto {
    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /**
     * 角色编码
     */
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /**
     * 显示顺序
     */
    @NotNull(message = "显示顺序不能为空")
    private Integer roleSort;

    /**
     * 状态 1启用 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Byte status;

    /**
     * 数据范围 ALL/DEPT_AND_CHILD/DEPT_SELF/SELF/CUSTOM
     */
    @NotBlank(message = "数据范围不能为空")
    private String dataScopeType;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 备注
     */
    private String remark;

}
