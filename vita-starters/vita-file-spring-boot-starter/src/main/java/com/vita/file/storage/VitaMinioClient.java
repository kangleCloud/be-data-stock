package com.vita.file.storage;

import io.minio.*;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * MinIO 客户端封装，用于访问受保护的底层方法。
 */
@Slf4j
public class VitaMinioClient extends MinioClient {

    public VitaMinioClient(MinioClient client) {
        super(client);
    }

    /**
     * 初始化分片上传。
     */
    public String initMultiPartUpload(String bucketName, String objectName) {
        try {
            CreateMultipartUploadResponse response = this.createMultipartUpload(
                    bucketName, null, objectName, null, null);
            return response != null ? response.result().uploadId() : null;
        } catch (Exception e) {
            log.warn("MinIO 初始化分片上传失败", e);
            return null;
        }
    }

    /**
     * 上传单个分片。
     */
    public void uploadPart(InputStream inputStream, String bucketName, String objectName,
                           String uploadId, int partNum) {
        try {
            this.uploadPart(bucketName, null, objectName, inputStream,
                    inputStream.available(), uploadId, partNum, null, null);
        } catch (Exception e) {
            log.warn("MinIO 上传分片失败", e);
        }
    }

    /**
     * 合并分片。
     */
    public ObjectWriteResponse completeMultipartUpload(String bucketName, String objectName,
                                                       String uploadId, Part[] parts) {
        try {
            return this.completeMultipartUpload(bucketName, null, objectName, uploadId, parts, null, null);
        } catch (Exception e) {
            log.warn("MinIO 合并分片失败", e);
            return null;
        }
    }

    /**
     * 列出分片。
     */
    public ListPartsResponse listParts(String bucketName, String objectName,
                                       Integer maxParts, Integer partNumberMarker, String uploadId) {
        try {
            return this.listParts(bucketName, null, objectName, maxParts, partNumberMarker, uploadId, null, null);
        } catch (Exception e) {
            log.warn("MinIO 获取分片列表失败", e);
            return null;
        }
    }

    /**
     * 取消分片上传。
     */
    public AbortMultipartUploadResponse abortMultipartUpload(String bucketName, String uploadId, String objectName) {
        try {
            return this.abortMultipartUpload(bucketName, null, objectName, uploadId, null, null);
        } catch (Exception e) {
            log.warn("MinIO 取消分片上传失败", e);
            return null;
        }
    }
}
