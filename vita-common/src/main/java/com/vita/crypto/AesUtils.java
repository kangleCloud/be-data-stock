package com.vita.crypto;

import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.crypto
 * @Author: znk
 * @CreateTime: 2026-03-20  21:44:10
 * @Description: AES 加密工具类，提供 AES 加密和解密的方法，使用 AES 算法对数据进行加密和解密，确保数据的安全性
 * @Version: 1.0
 */
public class AesUtils {

    private static Logger logger = LoggerFactory.getLogger(AesUtils.class);

    /**
     * 不能实例化
     */
    private AesUtils() {
    }

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String RULE_CBC = "AES/CBC/PKCS7Padding";
    private static final String ALGORITHM = "AES";
    private static final String RULE_ECB = "AES/ECB/PKCS7Padding";

    /**
     * AES加密 - ECB模式
     *
     * @param value  明文
     * @param secret 密钥
     * @return 加密后的Base64编码字符串
     */
    public static String encryptByEcbMode(String value, String secret) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(RULE_ECB);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(CHARSET));
            return BaseEncoding.base64().encode(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * AES解密 - ECB模式
     *
     * @param encryptedValue 加密后的Base64编码字符串
     * @param secret         密钥
     * @return 解密后的明文
     */
    public static String decryptByEcbMode(String encryptedValue, String secret) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(RULE_ECB);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            // Decode the base64 encoded string back to bytes
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedValue);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, CHARSET);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * AES加密 - ECB模式
     *
     * @param value  明文
     * @param secret 密钥
     * @return 加密后返回16进制
     */
    public static String encryptByEcbModeHex(String value, String secret) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(RULE_ECB);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(CHARSET));
            return byteArrayToHexString(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * AES解密 - ECB模式
     *
     * @param encrypted 加密后的16进制
     * @param secret    密钥
     * @return 解密后的明文
     */
    public static String decryptByEcbModeHex(String encrypted, String secret) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(RULE_ECB);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            byte[] encryptedBytes = hexStringToByteArray(encrypted);
            byte[] bytes = cipher.doFinal(encryptedBytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("16进制版AES解密异常:", e);
        }
        return null;
    }

    /**
     * AES加密 - CBC模式
     *
     * @param value  明文
     * @param secret 秘钥
     * @param iv     IV
     * @return String
     */
    public static String encryptByCbcMode(String value, String secret, String iv) {
        try {
            @SuppressWarnings("java:S3329")
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(CHARSET));
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(RULE_CBC);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            BaseEncoding baseEncoding = BaseEncoding.base64();
            return baseEncoding.encode(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * AES解密 - CBC模式
     *
     * @param encrypted 秘文
     * @param secret    秘钥
     * @param iv        IV
     * @return String
     */
    public static String decryptByCbcMode(String encrypted, String secret, String iv) {
        try {
            @SuppressWarnings("java:S3329")
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(CHARSET));
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(RULE_CBC);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] original = cipher.doFinal(BaseEncoding.base64().decode(encrypted));
            return new String(original);
        } catch (Exception ex) {
            logger.error("Base64版AES解密异常:", ex);
        }
        return null;
    }

    /**
     * AES加密 - CBC模式
     *
     * @param value  加密数据
     * @param secret 加密密钥
     * @param iv     偏移量
     * @description : AES加密返回16进制
     **/
    public static String encryptByCbcModeHex(String value, String secret, String iv) {
        try {
            @SuppressWarnings("java:S3329")
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(CHARSET));
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(RULE_CBC);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return byteArrayToHexString(encrypted);
        } catch (Exception e) {
            logger.error("16进制版AES加密异常:", e);
        }
        return null;
    }

    /**
     * AES解密 - CBC模式
     *
     * @description : AES解密返回16进制
     **/
    public static String decryptByCbcModeHex(String encrypted, String secret, String iv) {
        try {
            @SuppressWarnings("java:S3329")
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(CHARSET));
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(RULE_CBC);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] encryptedBytes = hexStringToByteArray(encrypted);
            byte[] bytes = cipher.doFinal(encryptedBytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("16进制版AES解密异常:", e);
        }
        return null;
    }

    /**
     * 16进制字符串转为字节数组
     *
     * @param hexString 16进制字符串
     */
    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) Integer.parseInt(hexString.substring(2 * i, 2 * i + 2), 16);
        }
        return result;
    }

    /**
     * 将byte转为字符串
     *
     * @param bytes byte数组
     */
    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        String encrypt = encryptByCbcMode("666666", "1234567812345678", "1234567812345678");
        System.out.println(encrypt);
        String decrypt = decryptByCbcMode("Hbt80+yDaFhBt/Yt798l5g==", "1234567812345678", "1234567812345678");
        System.out.println(decrypt);
    }
}
