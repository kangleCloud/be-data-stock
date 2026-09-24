package com.vita.file.support;

import cn.hutool.core.text.CharSequenceUtil;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * 文件访问 URL 组装支持。
 */
@Component
public class FileUrlSupport {

    public String resolveBaseUrl(String publicBaseUrl, String privateBaseUrl, boolean isPublic) {
        if (isPublic) {
            return normalizeBaseUrl(publicBaseUrl);
        }
        return normalizeBaseUrl(CharSequenceUtil.isNotBlank(privateBaseUrl) ? privateBaseUrl : publicBaseUrl);
    }

    public String join(String baseUrl, String... segments) {
        StringBuilder builder = new StringBuilder(normalizeBaseUrl(baseUrl));
        for (String segment : segments) {
            if (CharSequenceUtil.isBlank(segment)) {
                continue;
            }
            builder.append('/');
            builder.append(trimSlashes(segment));
        }
        return builder.toString();
    }

    public String buildStorageObjectUrl(String baseUrl, String objectKey, String expectedPrefix) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        String normalizedObjectKey = trimSlashes(objectKey);
        String normalizedPrefix = trimSlashes(expectedPrefix);
        if (CharSequenceUtil.isNotBlank(normalizedPrefix)
                && normalizedObjectKey.startsWith(normalizedPrefix + "/")
                && normalizedBaseUrl.endsWith("/" + normalizedPrefix)) {
            normalizedObjectKey = normalizedObjectKey.substring(normalizedPrefix.length() + 1);
        }
        return join(normalizedBaseUrl, normalizedObjectKey);
    }

    public String extractOrigin(String baseUrl) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        if (CharSequenceUtil.isBlank(normalizedBaseUrl)) {
            return "";
        }
        URI uri = URI.create(normalizedBaseUrl);
        StringBuilder builder = new StringBuilder();
        builder.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() > 0) {
            builder.append(":").append(uri.getPort());
        }
        return builder.toString();
    }

    public String rewriteUrlToBusinessObjectUrl(String originalUrl,
                                                String businessBaseUrl,
                                                String objectKey,
                                                String expectedPrefix) {
        String normalizedBusinessBase = normalizeBaseUrl(businessBaseUrl);
        if (CharSequenceUtil.isBlank(originalUrl) || CharSequenceUtil.isBlank(normalizedBusinessBase)) {
            return originalUrl;
        }
        URI originalUri = URI.create(originalUrl);
        String query = originalUri.getRawQuery();
        String rewritten = buildStorageObjectUrl(normalizedBusinessBase, objectKey, expectedPrefix);
        if (CharSequenceUtil.isNotBlank(query)) {
            rewritten += "?" + query;
        }
        return rewritten;
    }

    public String normalizeBaseUrl(String baseUrl) {
        if (CharSequenceUtil.isBlank(baseUrl)) {
            return "";
        }
        String normalized = baseUrl.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public String trimSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
