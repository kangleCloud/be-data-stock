package com.vita.auth.constant;

import java.time.format.DateTimeFormatter;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.constant
 * @Author: znk
 * @CreateTime: 2026-03-12  20:34:50
 * @Description: 认证相关常量类，定义与认证相关的常量值
 * @Version: 1.0
 */
public class AuthConstants {

    /**
     * 常量工具类不允许实例化。
     */
    private AuthConstants() {
        // 实例化
    }

    /**
     * Sa-Token Session 中缓存登录用户信息的键名。
     */
    public static final String LOGIN_USER_INFO = "LOGIN_USER_INFO";

    /**
     * Sa-Token Session 中缓存登录用户角色码集合的键名。
     */
    public static final String LOGIN_USER_ROLE = "LOGIN_USER_ROLE";

    /**
     * Sa-Token Session 中缓存登录用户权限码集合的键名。
     */
    public static final String LOGIN_USER_PERMISSION = "LOGIN_USER_PERMISSION";

    /**
     * 超级管理员虚拟角色码。
     */
    public static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";

    /**
     * 子管理员物理角色码。
     */
    public static final String SUB_ADMIN_ROLE_CODE = "SUB_ADMIN";

    /**
     * 全量权限通配符。
     */
    public static final String ALL_PERMISSION_CODE = "**:**:**";

    /**
     * IP 封禁标记在 Redis 中的统一前缀。
     */
    public static final String IP_BLOCK_KEY_PREFIX = "auth:ip:block:";

    /**
     * 用户名最小长度。
     */
    public static final int USERNAME_MIN_LENGTH = 4;

    /**
     * 用户名最大长度。
     */
    public static final int USERNAME_MAX_LENGTH = 32;

    /**
     * 密码最小长度。
     */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /**
     * 密码最大长度。
     */
    public static final int PASSWORD_MAX_LENGTH = 64;

    /**
     * 登录失败时的统一提示文案。
     */
    public static final String LOGIN_FAIL_MESSAGE = "用户名或密码错误";

    /**
     * 登录时间格式化器。
     */
    public static final DateTimeFormatter LOGIN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    /**
     * 构建指定 IP 的封禁缓存键。
     *
     * @param ip IP 地址
     * @return Redis 键
     */
    public static String buildIpBlockKey(String ip) {
        return IP_BLOCK_KEY_PREFIX + ip;
    }
}
