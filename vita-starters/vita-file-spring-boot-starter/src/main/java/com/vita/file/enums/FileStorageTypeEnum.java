package com.vita.file.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储类型枚举。
 */
@Getter
@AllArgsConstructor
public enum FileStorageTypeEnum {

    LOCAL("local", "本地磁盘存储"),
    MINIO("minio", "MinIO 对象存储");

    private final String code;
    private final String description;
}
