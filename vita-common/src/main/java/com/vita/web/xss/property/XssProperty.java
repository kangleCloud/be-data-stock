package com.vita.web.xss.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.List;

/** 请求层纯文本检查配置，私有配置由启动模块 profile YAML 提供。 */
@Data
@ConfigurationProperties(prefix = "vita.xss")
public class XssProperty {
    private boolean enabled = true;
    private List<String> urlPatterns = new ArrayList<>(List.of("/*"));
    private List<String> excludePaths = new ArrayList<>();
    /** 限制需缓存的 JSON／单个 multipart 文本，不改变上传文件的原有大小限制。 */
    private DataSize maxJsonBodySize = DataSize.ofMegabytes(10);
}
