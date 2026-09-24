package com.vita.core.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.enums
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 通用状态枚举
 * @Version: 1.0
 */
@Getter
public enum CommonStatusEnum {

    DISABLED((byte) 0, "禁用"),
    ENABLED((byte) 1, "启用");

    private final Byte code;
    private final String desc;

    CommonStatusEnum(Byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 判断当前状态是否为启用。
     *
     * @param code 状态编码
     * @return true 表示启用
     */
    public static boolean isEnabled(Byte code) {
        return Objects.equals(ENABLED.code, code);
    }

    /**
     * 判断当前状态是否为禁用。
     *
     * @param code 状态编码
     * @return true 表示禁用
     */
    public static boolean isDisabled(Byte code) {
        return Objects.equals(DISABLED.code, code);
    }
}
