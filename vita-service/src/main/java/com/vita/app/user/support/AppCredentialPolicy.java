package com.vita.app.user.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.support
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户名与密码规则
 * @Version: 1.0
 */
public final class AppCredentialPolicy {

    public static final int USER_NAME_MIN_LENGTH = 4;
    public static final int USER_NAME_MAX_LENGTH = 32;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 64;
    public static final int NICK_NAME_MAX_LENGTH = 64;
    public static final String LOGIN_FAILURE_MESSAGE = "用户名或密码错误";

    private static final Pattern USER_NAME_PATTERN = Pattern.compile("^[a-z0-9_]{4,32}$");

    private AppCredentialPolicy() {
    }

    /**
     * 用户名在进入数据库前统一去除首尾空白并转为小写，保证应用规则与唯一索引语义一致。
     *
     * @param userName 原始用户名
     * @return 规范化用户名
     */
    public static String normalizeAndValidateUserName(String userName) {
        if (CharSequenceUtil.isBlank(userName)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }
        String normalized = userName.trim().toLowerCase(Locale.ROOT);
        if (!USER_NAME_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "用户名只能包含4到32位小写字母、数字或下划线");
        }
        return normalized;
    }

    /**
     * 校验原始密码长度，密码不做裁剪或大小写转换。
     *
     * @param password 原始密码
     */
    public static void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "密码长度必须在8到64位之间");
        }
    }

    /**
     * 规范化昵称；未填写时使用已经规范化的用户名。
     *
     * @param nickName           原始昵称
     * @param normalizedUserName 规范化用户名
     * @return 可持久化昵称
     */
    public static String normalizeNickName(String nickName, String normalizedUserName) {
        String normalized = CharSequenceUtil.isBlank(nickName) ? normalizedUserName : nickName.trim();
        if (normalized.length() > NICK_NAME_MAX_LENGTH) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "昵称长度不能超过64位");
        }
        return normalized;
    }
}
