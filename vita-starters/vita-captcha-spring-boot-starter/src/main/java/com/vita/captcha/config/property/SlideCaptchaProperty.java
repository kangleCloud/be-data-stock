package com.vita.captcha.config.property;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.config.property
 * @Author: znk
 * @CreateTime: 2026-03-20  21:30:16
 * @Description: 滑动验证码属性类
 * @Version: 1.0
 */
@Data
public class SlideCaptchaProperty {
    private static final String DEFAULT_BACKGROUND_TEMPLATE = "https://loyer.wang/view/ftp/wallpaper/%s.jpg";

    /**
     * 背景图源路径，支持网络路径和本地绝对路径。
     */
    private String backImg = DEFAULT_BACKGROUND_TEMPLATE;
    /**
     * 背景图可访问的网络路径，本地文件时必须配置为可访问的网络地址模板。
     */
    private String backImgUrl = DEFAULT_BACKGROUND_TEMPLATE;
    /**
     * 图片长
     */
    private Integer imgWidth = 320;
    /**
     * 图片宽
     */
    private Integer imgHeight = 155;
    /**
     * 滑动块长
     */
    private Integer blockWidth = 65;
    /**
     * 滑动块宽
     */
    private Integer blockHeight = 55;
    /**
     * 滑动块圆形比率
     */
    private Integer blockRadius = 9;
    /**
     * 拼图验证码允许偏差
     */
    private Integer deviation = 3;

    /**
     * 应用默认值。
     */
    public void applyDefaults() {
        if (CharSequenceUtil.isBlank(backImg)) {
            backImg = DEFAULT_BACKGROUND_TEMPLATE;
        }
        if (imgWidth == null || imgWidth <= 0) {
            imgWidth = 320;
        }
        if (imgHeight == null || imgHeight <= 0) {
            imgHeight = 155;
        }
        if (blockWidth == null || blockWidth <= 0) {
            blockWidth = 65;
        }
        if (blockHeight == null || blockHeight <= 0) {
            blockHeight = 55;
        }
        if (blockRadius == null || blockRadius <= 0) {
            blockRadius = 9;
        }
        if (deviation == null || deviation < 0) {
            deviation = 3;
        }
        if (CharSequenceUtil.isBlank(backImgUrl) && SlideCaptchaProperty.isRemoteTemplate(backImg)) {
            backImgUrl = backImg;
        }
    }

    /**
     * 是否为网络路径模板。
     */
    public static boolean isRemoteTemplate(String value) {
        return CharSequenceUtil.startWithIgnoreCase(value, "http://")
                || CharSequenceUtil.startWithIgnoreCase(value, "https://");
    }
}
