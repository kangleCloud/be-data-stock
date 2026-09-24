package com.vita.system.sysDept.dto;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysDept.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:29:38
 * @Description: 部门表 创建 DTO
 * @version 1.0
 */
@Data
public class SysDeptCreateDto {
    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 祖级路径，如 0,100,101
     */
    private String ancestors;

    /**
     * 负责人ID
     */
    private Long leaderUserId;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 排序
     */
    private Integer sortNo;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 备注
     */
    private String remark;

}
