package com.vita.file.property;

import lombok.Data;

/**
 * MinIO 存储配置。
 */
@Data
public class MinioStorageProperty {

    /**
     * MinIO endpoint 地址。
     */
    private String endpoint = "http://127.0.0.1:9000";

    /**
     * MinIO access key。
     */
    private String accessKey = "minioadmin";

    /**
     * MinIO secret key。
     */
    private String secretKey = "minioadmin";

    /**
     * MinIO bucket 名称。
     */
    private String bucket = "vita-file";

    /**
     * MinIO 区域。
     */
    private String region = "us-east-1";

    /**
     * endpoint 未带协议时是否按 https 处理。
     */
    private Boolean secure = false;

    /**
     * bucket 不存在时是否自动创建。
     */
    private Boolean autoCreateBucket = false;

    /**
     * 预签名 URL 有效期，单位秒。
     */
    private Integer presignedExpireSeconds = 3600;

    /**
     * 是否按 path-style 访问。
     */
    private Boolean pathStyleAccess = true;

    /**
     * MinIO 公开访问地址前缀。
     */
    private String publicEndpoint;

    /**
     * MinIO 私有访问地址前缀，留空时回退到 publicEndpoint。
     */
    private String privateEndpoint;

    /**
     * 兼容旧字段：公开访问地址前缀。
     */
    private String publicDomain = "http://127.0.0.1:9000";

    /**
     * 兼容旧字段：私有访问地址前缀。
     */
    private String privateDomain;
}
