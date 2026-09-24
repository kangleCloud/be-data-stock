package com.vita.captcha.common.enums;

import com.vita.core.exception.ServiceException;
import lombok.Getter;

import java.util.Locale;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.enums
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码类型枚举
 */
public enum CaptchaTypeEnum {
    /**
     * 数组计算
     */
    MATH("math", "graphCaptchaService"),
    /**
     * 字符验证
     */
    CHAR("char", "graphCaptchaService"),
    /**
     * 滑动
     */
    SLIDE("slide", "slideCaptchaService"),
    /**
     * 纯滑动解锁
     */
    ALL_SLIDE("all_slide", "slideCaptchaService");

    /**
     * 验证码类型
     */
    @Getter
    private final String type;
    /**
     * 服务名称
     */
    @Getter
    private final String serviceName;

    CaptchaTypeEnum(String type, String serviceName) {
        this.type = type;
        this.serviceName = serviceName;
    }

    /**
     * 根据验证吗
     *
     * @param captchaType 验证码类型
     * @return
     */
    public static String getService(String captchaType) {
        return fromType(captchaType).getServiceName();
    }

    /**
     * 规范化验证码类型，默认值为 all_slide。
     */
    public static String normalize(String captchaType) {
        if (captchaType == null) {
            return ALL_SLIDE.type;
        }
        String normalized = captchaType.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? ALL_SLIDE.type : normalized;
    }

    /**
     * 解析验证码类型。
     *
     * @param captchaType 原始验证码类型
     * @return 枚举值
     */
    public static CaptchaTypeEnum fromType(String captchaType) {
        String normalized = normalize(captchaType);
        for (CaptchaTypeEnum captchaTypeEnum : CaptchaTypeEnum.values()) {
            if (captchaTypeEnum.getType().equals(normalized)) {
                return captchaTypeEnum;
            }
        }
        throw new ServiceException("不支持的验证码类型:" + captchaType);
    }

    /**
     * 是否为图形验证码类型。
     */
    public static boolean isGraphType(String captchaType) {
        CaptchaTypeEnum captchaTypeEnum = fromType(captchaType);
        return captchaTypeEnum == CHAR || captchaTypeEnum == MATH;
    }

    /**
     * 是否为滑动验证码类型。
     */
    public static boolean isSlideType(String captchaType) {
        CaptchaTypeEnum captchaTypeEnum = fromType(captchaType);
        return captchaTypeEnum == SLIDE || captchaTypeEnum == ALL_SLIDE;
    }
}
