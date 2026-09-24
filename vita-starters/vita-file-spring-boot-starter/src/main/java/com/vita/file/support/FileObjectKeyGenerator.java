package com.vita.file.support;

import cn.hutool.core.util.IdUtil;
import com.vita.file.property.FileProperty;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 对象键生成器：{visibilityPrefix}/{prefixDir?}/{yyyy/MM/dd}/{UUID}.{ext}。
 */
@Component
public class FileObjectKeyGenerator {

    /**
     * 生成对象键。
     *
     * @param fileProperty 文件配置
     * @param isPublic     是否公开
     * @param suffix       文件后缀（不含点号）
     * @param prefixDir    前置目录（可选）
     * @return 对象键
     */
    public String generate(FileProperty fileProperty, boolean isPublic, String suffix, String prefixDir) {
        String visibilityPrefix = isPublic ? fileProperty.getPublicPrefix() : fileProperty.getPrivatePrefix();
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String fileName = IdUtil.fastSimpleUUID() + "." + suffix;

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(visibilityPrefix);
        if (prefixDir != null && !prefixDir.isEmpty()) {
            keyBuilder.append("/").append(prefixDir);
        }
        keyBuilder.append("/").append(datePath).append("/").append(fileName);
        return keyBuilder.toString();
    }
}
