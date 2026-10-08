package com.vita.file.config;

import com.vita.file.storage.VitaMinioClient;
import lombok.Getter;

/** 当前 MinIO 配置及其客户端实例。 */
@Getter
public class MinioClientContext {
    private final ResolvedMinioConfig config;
    private final VitaMinioClient uploadClient;
    private final VitaMinioClient signClient;
    private volatile boolean bucketReady;

    public MinioClientContext(ResolvedMinioConfig config, VitaMinioClient uploadClient,
                              VitaMinioClient signClient) {
        this.config = config;
        this.uploadClient = uploadClient;
        this.signClient = signClient;
    }

    public boolean isBucketReady() {
        return bucketReady;
    }

    public void setBucketReady(boolean bucketReady) {
        this.bucketReady = bucketReady;
    }
}
