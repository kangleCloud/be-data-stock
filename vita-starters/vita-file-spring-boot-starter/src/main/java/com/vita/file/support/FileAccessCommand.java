package com.vita.file.support;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 文件访问命令对象。
 */
@Data
@Builder
public class FileAccessCommand {

    /**
     * 对象键。
     */
    private String objectKey;

    /**
     * 是否公开。
     */
    private Boolean isPublic;

    /**
     * 额外请求参数（签名时使用）。
     */
    private Map<String, String> extraParams;
}
