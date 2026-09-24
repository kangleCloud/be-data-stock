package com.vita.auth.model;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.model
 * @Author: znk
 * @CreateTime: 2026-03-06  21:10:42
 * @Description: 登录用户信息模型
 * @Version: 1.0
 */
@Data
public class LoginUserInfoModel {

    /**
     * ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 验证码
     */
    private String code;

    /**
     * 唯一标识
     */
    private String uuid;

    /**
     * 令牌
     */
    private String token;

    /**
     * 令牌前缀/键名（便于前端存储）
     */
    private String tokenName;

    /**
     * 令牌前缀（如 Bearer，便于前端组合完整请求头）
     */
    private String tokenPrefix;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 是否超级管理员 1是 0否
     */
    private Byte isSuperAdmin;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 登录 IP 地址
     */
    private String loginAddress;

    /**
     * 登录地址
     */
    private String loginLocation;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 登录时间
     */
    private String loginTime;
}
