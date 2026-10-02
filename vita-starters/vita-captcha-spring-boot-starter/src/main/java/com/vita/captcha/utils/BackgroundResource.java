package com.vita.captcha.utils;

import java.awt.image.BufferedImage;

/** 滑块验证码背景图及其访问地址。 */
public final class BackgroundResource {
    private final BufferedImage bufferedImage;
    private final String accessUrl;

    public BackgroundResource(BufferedImage bufferedImage, String accessUrl) {
        this.bufferedImage = bufferedImage;
        this.accessUrl = accessUrl;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public String getAccessUrl() {
        return accessUrl;
    }
}
