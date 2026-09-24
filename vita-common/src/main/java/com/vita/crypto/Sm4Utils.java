package com.vita.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Base64;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.crypto
 * @Author: znk
 * @CreateTime: 2026-03-06  19:34:25
 * @Description: sm4加密算法工具类
 * @Version: 1.0
 */
@Slf4j
public class Sm4Utils {

    /**
     * sm4 用于加密的密钥字符串，必须是16字节（128位）的UTF-8编码
     */
    private static final String key = "Vita-4LQyrt4PbJE";

    /**
     * SM4算法相关常量
     */
    private static final String KEY_ALGORITHM = "SM4";
    /**
     * SM4加密算法的转换格式，使用ECB模式和PKCS5Padding填充方式
     */
    private static final String TRANSFORMATION = "SM4/ECB/PKCS5Padding";
    /**
     * Bouncy Castle提供者名称
     */
    private static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;
    /**
     * SM4算法要求的密钥长度为16字节（128位）
     */
    private static final int KEY_LENGTH_BYTES = 16;

    static {
        /**
         * 确保Bouncy Castle提供者已注册，如果未注册则添加它，以支持SM4算法的加密和解密操作
         */
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Sm4Utils() {
    }

    /**
     * 使用SM4算法加密明文，并将结果编码为Base64字符串
     *
     * @param plainText 要加密的明文字符串
     * @return 加密后的Base64字符串
     */
    public static String encryptToBase64(String plainText) {
        requireNonEmpty(plainText, "plainText");
        byte[] keyBytes = validateAndGetKeyBytes(key);
        try {
            Cipher cipher = buildCipher(Cipher.ENCRYPT_MODE, keyBytes);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SM4 encrypt failed", e);
        }
    }

    /**
     * 使用SM4算法解密Base64编码的密文，并返回解密后的明文字符串
     *
     * @param cipherText 要解密的Base64编码的密文字符串
     * @return 解密后的明文字符串
     */
    public static String decryptFromBase64(String cipherText) {
        requireNonEmpty(cipherText, "cipherText");
        byte[] keyBytes = validateAndGetKeyBytes(key);
        final byte[] encryptedBytes;
        try {
            encryptedBytes = Base64.getDecoder().decode(cipherText);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("cipherText is not valid Base64", e);
        }
        try {
            Cipher cipher = buildCipher(Cipher.DECRYPT_MODE, keyBytes);
            byte[] plainBytes = cipher.doFinal(encryptedBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SM4 decrypt failed", e);
        }
    }

    /**
     * 构建并初始化Cipher对象，根据指定的模式（加密或解密）和密钥字节数组，返回一个可用于执行SM4加密或解密操作的Cipher实例
     *
     * @param mode     Cipher.ENCRYPT_MODE或Cipher.DECRYPT_MODE，指定Cipher的操作模式
     * @param keyBytes 用于加密或解密的密钥字节数组，必须是16字节（128位）
     * @return 初始化后的Cipher对象
     * @throws GeneralSecurityException 如果在构建或初始化Cipher过程中发生任何加密相关的异常，例如无效的密钥长度、算法不支持等
     */
    private static Cipher buildCipher(int mode, byte[] keyBytes) throws GeneralSecurityException {
        SecretKey keySpec = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER_NAME);
        cipher.init(mode, keySpec);
        return cipher;
    }

    /**
     * 验证密钥字符串是否符合SM4算法的要求（必须是16字节的UTF-8编码），并将其转换为字节数组返回，如果验证失败则抛出IllegalArgumentException异常
     *
     * @param key 用于加密或解密的密钥字符串，必须是16字节（128位）的UTF-8编码
     * @return 符合要求的密钥字节数组
     */
    private static byte[] validateAndGetKeyBytes(String key) {
        requireNonEmpty(key, "key");
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("key must be exactly 16 bytes in UTF-8");
        }
        return keyBytes;
    }

    /**
     * 验证字符串是否为null或空字符串，如果是则抛出IllegalArgumentException异常，参数name用于指定被验证的参数名称，以便在异常消息中提供更清晰的错误信息
     *
     * @param value 要验证的字符串值
     * @param name  被验证参数的名称，用于异常消息中指明哪个参数不合法
     * @throws IllegalArgumentException 如果value为null或空字符串，则抛出此异常，消息内容为"{name} must not be empty"
     */
    private static void requireNonEmpty(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    public static void main(String[] args) {
        String key = "Vita-4LQyrt4PbJE";
        // 待加密明文String plainText = "Hello, SM4!";
        String plainText = "admin@123456";
        // 执行加密并输出密文String cipherText = encryptToBase64(plainText, key);
        String cipherText = encryptToBase64(plainText);
        log.info("加密输出: {}", cipherText);
        // 执行解密并输出明文String decryptedText = decryptFromBase64(cipherText, key);
        String decryptedText = decryptFromBase64(cipherText);
        log.info("解密输出：{}", decryptedText);
    }
}
