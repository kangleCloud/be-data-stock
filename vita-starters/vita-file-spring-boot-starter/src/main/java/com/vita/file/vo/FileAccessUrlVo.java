package com.vita.file.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文件访问 URL 结果 VO。
 */
@Data
@Builder
public class FileAccessUrlVo {

    /**
     * 对象键。
     */
    private String objectKey;

    /**
     * 访问 URL（公开文件直接返回，私有文件为签名 URL）。
     */
    private String accessUrl;

    /**
     * 是否公开。
     */
    private Boolean isPublic;
}
