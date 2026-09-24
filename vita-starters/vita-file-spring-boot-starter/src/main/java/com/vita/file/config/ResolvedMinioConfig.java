package com.vita.file.config;

import lombok.Builder;
import lombok.Value;

/**
 * MinIO 生效配置快照。
 */
@Value
@Builder
public class ResolvedMinioConfig {

    String endpoint;

    String accessKey;

    String secretKey;

    String bucket;

    String region;

    boolean secure;

    boolean autoCreateBucket;

    int presignedExpireSeconds;

    boolean pathStyleAccess;

    String publicEndpoint;

    String privateEndpoint;
}
