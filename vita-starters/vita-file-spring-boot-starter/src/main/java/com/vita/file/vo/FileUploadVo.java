package com.vita.file.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传结果 VO。
 */
@Data
@Builder
public class FileUploadVo {

    /**
     * 桶名（MinIO 模式）或 bucket 标识。
     */
    private String bucketName;

    /**
     * 主机名 / 访问域名。
     */
    private String hostName;

    /**
     * 对象键路径。
     */
    private String uri;

    /**
     * 公开文件可直接访问的完整 URL。
     */
    private String fileUrl;

    /**
     * 私有文件临时签名 URL。
     */
    private String signUrl;

    /**
     * 上传 ID。
     */
    private String uploadId;
}
