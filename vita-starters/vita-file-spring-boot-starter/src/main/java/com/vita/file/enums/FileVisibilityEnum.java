package com.vita.file.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件可见性枚举。
 */
@Getter
@AllArgsConstructor
public enum FileVisibilityEnum {

    PUBLIC(1, "公开访问"),
    PRIVATE(2, "私有访问");

    private final int code;
    private final String description;

    public static boolean isPublic(Integer code) {
        return code != null && code == PUBLIC.code;
    }
}
