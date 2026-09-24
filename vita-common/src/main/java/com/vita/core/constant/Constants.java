package com.vita.core.constant;

import com.vita.core.enums.CommonStatusEnum;

import java.util.Locale;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.constant
 * @Author: znk
 * @CreateTime: 2026-03-12  20:52:22
 * @Description: 常量类，定义系统中使用的常量值，例如错误码、默认值等
 * @Version: 1.0
 */
public class Constants {

    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * 系统语言
     */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 验证码有效期（分钟）
     */
    public static final Integer CAPTCHA_EXPIRATION = 2;

    /**
     * 通用启用标识
     */
    public static final Byte ENABLED = CommonStatusEnum.ENABLED.getCode();

    /**
     * 通用禁用标识
     */
    public static final Byte DISABLED = CommonStatusEnum.DISABLED.getCode();

    /**
     * 校验是否唯一的返回标识
     */
    public static final boolean UNIQUE = true;
    public static final boolean NOT_UNIQUE = false;
}
