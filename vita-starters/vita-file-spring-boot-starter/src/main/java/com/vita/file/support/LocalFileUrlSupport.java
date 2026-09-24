package com.vita.file.support;

import com.vita.file.property.FileProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件 URL 解析与下载支持。
 */
@Component
public class LocalFileUrlSupport {

    private final FileProperty fileProperty;

    public LocalFileUrlSupport(FileProperty fileProperty) {
        this.fileProperty = fileProperty;
    }

    /**
     * 根据对象键获取本地文件。
     */
    public File resolveFile(String objectKey) {
        String basePath = fileProperty.getLocal().getBasePath();
        Path filePath = Paths.get(basePath, objectKey);
        return filePath.toFile();
    }

    /**
     * 获取文件输入流。
     */
    public InputStream getInputStream(String objectKey) throws IOException {
        File file = resolveFile(objectKey);
        if (!file.exists() || !file.canRead()) {
            throw new IOException("文件不存在或不可读: " + objectKey);
        }
        return new FileInputStream(file);
    }

    /**
     * 根据对象键构建本地文件绝对路径。
     */
    public String getAbsolutePath(String objectKey) {
        return resolveFile(objectKey).getAbsolutePath();
    }

    /**
     * 确保父目录存在。
     */
    public void ensureParentDirExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
