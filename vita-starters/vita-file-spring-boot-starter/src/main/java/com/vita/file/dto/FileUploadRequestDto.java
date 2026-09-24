package com.vita.file.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 文件上传请求 DTO。
 */
@Data
public class FileUploadRequestDto {

    /**
     * 是否公开: 1=公开, 2=私有。
     */
    @NotNull(message = "是否公开不能为空")
    private Integer isPublic;

    /**
     * 是否分片上传: 1=分片, 0=单文件。
     */
    @NotNull(message = "是否分片不能为空")
    private Integer isPart;

    /**
     * 文件后缀格式（不含点号，如 "jpg"）。
     */
    @NotEmpty(message = "文件后缀名不能为空")
    private String suffix;

    /**
     * 源文件尺寸（字节），分片上传时必传。
     */
    @NotNull(message = "源文件大小不能为空")
    private Long fileSize;

    /**
     * 前置附加目录（单层，长度不超过 30 字符）。
     */
    private String prefixDir;

    /**
     * 原始文件名。
     */
    private String originName;

    /**
     * 上传 ID（上传/分片时必传）。
     */
    private String uploadId;

    /**
     * 分片序号（分片上传时必传）。
     */
    private Integer partNum;

    /**
     * 分片总数（分片上传时必传）。
     */
    private Integer totalParts;
}
