package com.vita.file.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传会话缓存对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadSession {

    /**
     * 上传 ID。
     */
    private String uploadId;

    /**
     * 文件对象键。
     */
    private String objectKey;

    /**
     * 是否公开。
     */
    private Integer isPublic;

    /**
     * 是否分片上传。
     */
    private Integer isPart;

    /**
     * 文件后缀。
     */
    private String suffix;

    /**
     * 文件大小。
     */
    private Long fileSize;

    /**
     * 前置目录。
     */
    private String prefixDir;

    /**
     * 原始文件名。
     */
    private String originName;
}
