package com.vita.file.property;

import lombok.Data;

/**
 * 文件签名配置（用于本地存储私有文件的临时签名 URL）。
 */
@Data
public class FileSignatureProperty {

    /**
     * 签名密钥。
     */
    private String secret = "change-me";
}
