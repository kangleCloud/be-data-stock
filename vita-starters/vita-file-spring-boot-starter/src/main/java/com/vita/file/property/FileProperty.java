package com.vita.file.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储主配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "vita.file")
public class FileProperty {

    /**
     * 是否启用文件上传 starter。
     */
    private Boolean enabled = true;

    /**
     * 当前激活的存储类型: local | minio。
     */
    private String active = "local";

    /**
     * 公开文件对象键前缀。
     */
    private String publicPrefix = "public";

    /**
     * 私有文件对象键前缀。
     */
    private String privatePrefix = "private";

    /**
     * 最大上传大小，单位字节。
     */
    private Long maxFileSize = 10485760L;

    /**
     * 私有访问签名有效期，单位秒。
     */
    private Integer signExpireSeconds = 300;

    /**
     * 本地存储配置。
     */
    private LocalStorageProperty local = new LocalStorageProperty();

    /**
     * MinIO 存储配置。
     */
    private MinioStorageProperty minio = new MinioStorageProperty();

    /**
     * 签名配置。
     */
    private FileSignatureProperty signature = new FileSignatureProperty();
}
