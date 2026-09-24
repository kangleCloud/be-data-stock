package com.vita.file.support;

import cn.hutool.core.text.CharSequenceUtil;

import java.util.Set;

/**
 * 固定允许上传的文件后缀白名单。
 */
public final class FileExtensionWhitelist {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "pdf",
            "doc", "docx", "xls", "xlsx", "csv", "txt", "zip",
            "rar", "7z", "mp4"
    );

    private FileExtensionWhitelist() {
    }

    public static boolean isAllowed(String suffix) {
        if (CharSequenceUtil.isBlank(suffix)) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(suffix.trim().toLowerCase());
    }
}
