package com.vita.system.sysUser.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.vo
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 列表 VO
 * @version 1.0
 */
@Data
public class SysUserListVo {
    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 性别(0-女,1-男,2-其他)
     */
    private String gender;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 是否超级管理员 1是 0否
     */
    private Byte isSuperAdmin;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 上次登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 登录地址
     */
    private String loginAddress;

    /**
     * 密码最后更新时间
     */
    private java.time.LocalDateTime pwdUpdateDate;

    /**
     * 备注
     */
    private String remark;

}
