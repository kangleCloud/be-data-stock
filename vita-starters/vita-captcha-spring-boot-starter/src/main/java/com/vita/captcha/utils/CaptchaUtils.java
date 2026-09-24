package com.vita.captcha.utils;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.captcha.common.enums.CaptchaTypeEnum;
import com.vita.captcha.config.property.CaptchaProperty;
import com.vita.core.exception.ServiceException;
import com.vita.crypto.AesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.utils
 * @Author: znk
 * @CreateTime: 2026-03-20  21:24:09
 * @Description: 验证码工具类
 * @Version: 1.0
 */
public class CaptchaUtils {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaUtils.class);

    /**
     * 验证码加密
     *
     * @param mix             混合值
     * @param captchaCode     tuxi验证码
     * @param captchaProperty 验证码属性
     **/
    public static String aesEncode(String mix, String captchaCode, CaptchaProperty captchaProperty) {
        captchaProperty.normalizeAndValidate();
        if (CharSequenceUtil.isBlank(mix)) {
            throw new ServiceException("混淆值加密时不能为空,请传入默认值");
        }
        if (CharSequenceUtil.isBlank(captchaCode)) {
            throw new ServiceException("验证码加密数据不能为空");
        }
        long expiredAt = System.currentTimeMillis() / 1000;
        Integer expired = captchaProperty.getExpired();
        expiredAt += expired * 60;
        String encryptDataStr = CaptchaTypeEnum.normalize(captchaProperty.getCaptchaType()) + ":" + mix + ":" + captchaCode + ":" + expiredAt;
        String encryptData = AesUtils.encryptByCbcMode(encryptDataStr, captchaProperty.getAesSecret(), captchaProperty.getAesIv());
        if (CharSequenceUtil.isBlank(encryptData)) {
            throw new ServiceException("验证码加密失败");
        }
        return encryptData;
    }

    /**
     * aes解密
     *
     * @param mix             混淆值，用户名登录必须为登录名，手机登录必须为手机号，其他为空
     * @param encryptData     解密数据
     * @param captchaProperty 验证码属性
     */
    public static String aesDecrypt(String mix, String encryptData, CaptchaProperty captchaProperty) {
        captchaProperty.normalizeAndValidate();
        String decypt = AesUtils.decryptByCbcMode(encryptData, captchaProperty.getAesSecret(), captchaProperty.getAesIv());
        if (CharSequenceUtil.isBlank(decypt)) {
            logger.warn("图形验证码解码数据为空");
            throw new ServiceException("图形验证码校验失败");
        }
        String[] decrypts = decypt.split(":");

        int length = 4;
        int index1 = 1;
        int index2 = 2;
        int index3 = 3;
        if (decrypts.length != length || !mix.equals(decrypts[index1]) || CharSequenceUtil.isBlank(decrypts[index2]) || decrypts[index3] == null) {
            logger.warn("图形验证码解码失败：{},长度:{},mix值:{}", decrypts, decrypts.length, mix);
            throw new ServiceException("图形验证码错误");
        }
        if (CharSequenceUtil.isBlank(decrypts[0])) {
            logger.warn("图形验证解码的验证码类型错误,{}", decrypts[0]);
            throw new ServiceException("验证码类型错误");
        }
        if (!CaptchaTypeEnum.normalize(captchaProperty.getCaptchaType()).equals(decrypts[0])) {
            logger.warn("图形验证解码的验证码类型错误,解码类型:{},配置类型:{}", decrypts[0], captchaProperty.getCaptchaType());
            throw new ServiceException("图形验证失败");
        }
        long expireAt = Long.parseLong(decrypts[3]);
        long nowTime = System.currentTimeMillis() / 1000;
        if (nowTime > expireAt) {
            throw new ServiceException("图形验证码已失效");
        }
        return decrypts[2];
    }

    /**
     * 校验验证码属性
     *
     * @param captchaProperty 验证码属性
     */
    public static void checkCaptchaProperty(CaptchaProperty captchaProperty) {
        if (captchaProperty == null) {
            throw new ServiceException("验证码属性未配置");
        }
    }

    /**
     * 校验验证码启用状态。
     */
    public static void checkCaptchaEnabled(CaptchaProperty captchaProperty) {
        checkCaptchaProperty(captchaProperty);
        captchaProperty.normalizeAndValidate();
        if (!captchaProperty.isEnabled()) {
            throw new ServiceException("验证码未启用");
        }
    }

    /**
     * 校验图形配置
     */
    public static void checkSlideConfig(CaptchaProperty captchaProperty) {
        checkCaptchaEnabled(captchaProperty);
        if (Objects.isNull(captchaProperty.getSlide())) {
            throw new ServiceException("图形验证码未配置");
        }
    }

    /**
     * 生成 uuid。
     */
    public static String resolveUuid(String uuid) {
        return CharSequenceUtil.isBlank(uuid)
                ? UUID.randomUUID().toString().replace("-", "")
                : uuid;
    }

    /**
     * 根据 mix 和 uuid 生成最终混淆值。
     */
    public static String resolveMix(String mix, String uuid) {
        return CharSequenceUtil.isBlank(mix) ? uuid : mix;
    }

    /**
     * 图片转 Base64。
     */
    public static String toBase64(BufferedImage bufferedImage, String type) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, type, byteArrayOutputStream);
            String base64 = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
            return String.format("data:image/%s;base64,%s", type, base64);
        } catch (IOException e) {
            logger.warn("验证码图片转换 BASE64 失败", e);
            throw new ServiceException("验证码生成失败");
        }
    }
}
