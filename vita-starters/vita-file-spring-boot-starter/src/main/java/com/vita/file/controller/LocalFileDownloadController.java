package com.vita.file.controller;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.file.property.FileProperty;
import com.vita.file.support.FileSignatureSupport;
import com.vita.file.support.LocalFileUrlSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 本地文件下载控制器（仅 active=local 时生效）。
 */
@Slf4j
@RestController
@RequestMapping("/file/download")
@ConditionalOnProperty(name = "vita.file.active", havingValue = "local")
public class LocalFileDownloadController {

    private final FileProperty fileProperty;
    private final FileSignatureSupport signatureSupport;
    private final LocalFileUrlSupport localFileUrlSupport;

    public LocalFileDownloadController(FileProperty fileProperty,
                                       FileSignatureSupport signatureSupport,
                                       LocalFileUrlSupport localFileUrlSupport) {
        this.fileProperty = fileProperty;
        this.signatureSupport = signatureSupport;
        this.localFileUrlSupport = localFileUrlSupport;
    }

    /**
     * 下载本地文件（私有文件需携带签名）。
     *
     * @param bucket bucket 名称
     * @param key    对象键路径
     * @param token  签名 Token
     * @param time   签名时间戳
     */
    @GetMapping("/{bucket}/**")
    public void download(@PathVariable String bucket,
                         @RequestParam(required = false) String token,
                         @RequestParam(required = false) String time,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String objectKey = extractObjectKey(bucket, request);
        if (CharSequenceUtil.isBlank(objectKey)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        File file = localFileUrlSupport.resolveFile(objectKey);
        if (!file.exists() || !file.canRead()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String privatePrefix = fileProperty.getPrivatePrefix();
        if (objectKey.startsWith(privatePrefix + "/")) {
            String uri = "/file/download/" + bucket + "/" + objectKey;
            if (!signatureSupport.verifyToken(fileProperty, uri, token, time)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        // 流式返回文件
        response.setContentType(getContentType(file.getName()));
        response.setContentLengthLong(file.length());
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");

        try (InputStream inputStream = localFileUrlSupport.getInputStream(objectKey);
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.warn("文件下载失败, key={}", objectKey, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String extractObjectKey(String bucket, HttpServletRequest request) {
        String path = request.getRequestURI();
        String marker = "/file/download/" + bucket + "/";
        int index = path.indexOf(marker);
        if (index < 0) {
            return "";
        }
        return path.substring(index + marker.length());
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        }
        return "application/octet-stream";
    }
}
