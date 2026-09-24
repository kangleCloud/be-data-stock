package com.vita.file.support;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传命令对象，封装单次上传所需的全部参数。
 */
@Data
@Builder
public class FileUploadCommand {

    /**
     * 对象键（完整路径）。
     */
    private String objectKey;

    /**
     * 是否公开。
     */
    private Boolean isPublic;

    /**
     * 上传 ID（分片上传时必传）。
     */
    private String uploadId;

    /**
     * 分片序号。
     */
    private Integer partNum;

    /**
     * 分片总数。
     */
    private Integer totalParts;

    /**
     * 分片大小。
     */
    private Long partSize;
}
