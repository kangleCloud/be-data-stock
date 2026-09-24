package com.vita.file.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.file.property.FileProperty;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.MessageDigest;

/**
 * 本地文件签名支持：MD5(uri + timestamp + secretKey) 生成临时访问 Token。
 */
@Component
public class FileSignatureSupport {

    private static final long DEFAULT_EXPIRE_SECONDS = 300;

    /**
     * 生成签名 Token。
     */
    public String generateToken(FileProperty fileProperty, String uri) {
        String secret = fileProperty.getSignature().getSecret();
        if (CharSequenceUtil.isBlank(secret) || "change-me".equals(secret)) {
            throw new IllegalArgumentException("签名密钥未正确配置");
        }
        long timestamp = System.currentTimeMillis() / 1000;
        String raw = md5Hex(uri + timestamp) + secret;
        String token = md5Hex(raw);
        return token + "&time=" + timestamp;
    }

    /**
     * 校验签名 Token。
     */
    public boolean verifyToken(FileProperty fileProperty, String uri, String token, String timestampStr) {
        if (CharSequenceUtil.isBlank(token) || CharSequenceUtil.isBlank(timestampStr)) {
            return false;
        }
        String secret = fileProperty.getSignature().getSecret();
        if (CharSequenceUtil.isBlank(secret) || "change-me".equals(secret)) {
            return false;
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            return false;
        }
        int expireSeconds = fileProperty.getSignExpireSeconds() != null
                ? fileProperty.getSignExpireSeconds() : (int) DEFAULT_EXPIRE_SECONDS;
        if (System.currentTimeMillis() / 1000 - timestamp > expireSeconds) {
            return false;
        }
        String expectedToken = md5Hex(md5Hex(uri + timestamp) + secret);
        return token.equals(expectedToken);
    }

    /**
     * 构建带签名的完整 URL。
     */
    public String buildSignedUrl(FileProperty fileProperty, String baseUrl, String uri) {
        String token = generateToken(fileProperty, uri);
        return baseUrl + uri + "?token=" + token;
    }

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }
    }
}
