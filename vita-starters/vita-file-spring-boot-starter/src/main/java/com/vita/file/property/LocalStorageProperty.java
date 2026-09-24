package com.vita.file.property;

import lombok.Data;

/**
 * 本地存储配置。
 */
@Data
public class LocalStorageProperty {

    /**
     * 本地文件落盘根目录。
     */
    private String basePath = "/data/vita/uploads";

    /**
     * 公开访问域名前缀。
     */
    private String publicDomain = "http://127.0.0.1:19001";

    /**
     * 私有访问域名前缀，留空时回退到 publicDomain。
     */
    private String privateDomain;
}
