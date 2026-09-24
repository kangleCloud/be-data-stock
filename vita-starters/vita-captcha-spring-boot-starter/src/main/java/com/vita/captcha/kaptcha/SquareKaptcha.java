package com.vita.captcha.kaptcha;

import com.google.code.kaptcha.GimpyEngine;
import com.google.code.kaptcha.util.Configurable;

import java.awt.image.BufferedImage;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.kaptcha
 * @Author: znk
 * @CreateTime: 2026-03-20  21:32:44
 * @Description: 验证码原版方形样式
 * @Version: 1.0
 */
public class SquareKaptcha extends Configurable implements GimpyEngine {

    /**
     * 获取扭曲图像
     *
     * @param baseImage
     * @return
     */
    @Override
    public BufferedImage getDistortedImage(BufferedImage baseImage) {
        return baseImage;
    }
}
