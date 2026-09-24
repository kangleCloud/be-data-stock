package com.vita.file.support;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件访问结果。
 */
@Data
@Builder
public class FileAccessResult {

    /**
     * 访问 URL。
     */
    private String accessUrl;

    /**
     * 是否为签名 URL。
     */
    private Boolean signed;

    /**
     * 分片列表（仅 listParts 时返回）。
     */
    private List<String> parts;
}
