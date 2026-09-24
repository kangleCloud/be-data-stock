package com.vita.file.storage;

import com.vita.file.support.FileAccessCommand;
import com.vita.file.support.FileAccessResult;
import com.vita.file.support.FileUploadCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储策略接口。
 */
public interface FileStorage {

    /**
     * 获取存储类型标识。
     */
    String getType();

    /**
     * 上传单个文件。
     */
    void upload(MultipartFile file, FileUploadCommand command);

    /**
     * 通过流上传。
     */
    void uploadByStream(InputStream inputStream, FileUploadCommand command);

    /**
     * 初始化分片上传。
     *
     * @return uploadId
     */
    String initMultipartUpload(FileUploadCommand command);

    /**
     * 上传单个分片。
     */
    void uploadPart(MultipartFile file, FileUploadCommand command);

    /**
     * 完成分片上传（合并分片）。
     */
    void completeMultipartUpload(FileUploadCommand command);

    /**
     * 取消分片上传。
     */
    void abortMultipartUpload(String uploadId, String objectKey);

    /**
     * 生成访问 URL。
     */
    FileAccessResult generateAccessUrl(FileAccessCommand command);

    /**
     * 构建直接访问 URL（不带签名）。
     */
    String buildDirectUrl(FileAccessCommand command);

    /**
     * 列出已上传的分片。
     */
    List<String> listParts(FileUploadCommand command);

    /**
     * 获取已上传分片总大小。
     */
    long getUploadedPartTotalSize(FileUploadCommand command);

    /**
     * 获取桶名。
     */
    String getBucketName(boolean isPublic);

    /**
     * 获取访问域名。
     */
    String getHostname(boolean isPublic);
}
