package com.vita.captcha.config;

import com.google.code.kaptcha.GimpyEngine;
import com.google.code.kaptcha.util.Configurable;
import com.jhlabs.image.RippleFilter;
import com.jhlabs.image.ShadowFilter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.config
 * @Author: znk
 * @CreateTime: 2026-03-20  22:12:31
 * @Description: 图形验证码阴影控制
 * @Version: 1.0
 */
public class GraphShadowGimpy extends Configurable implements GimpyEngine {
    private static final Random RANDOM = new Random();

    public GraphShadowGimpy() {
    }

    @Override
    public BufferedImage getDistortedImage(BufferedImage baseImage) {
        BufferedImage distortedImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), 2);
        Graphics2D graph = (Graphics2D) distortedImage.getGraphics();
        ShadowFilter shadowFilter = new ShadowFilter();
        shadowFilter.setRadius(10.0F);
        shadowFilter.setDistance(5.0F);
        shadowFilter.setOpacity(1.0F);
        RippleFilter rippleFilter = new RippleFilter();
        rippleFilter.setWaveType(0);
        rippleFilter.setXAmplitude(7.6F);
        rippleFilter.setYAmplitude(RANDOM.nextFloat() + 1.0F);
        rippleFilter.setXWavelength((float) (RANDOM.nextInt(7) + 8));
        rippleFilter.setYWavelength((float) (RANDOM.nextInt(3) + 2));
        rippleFilter.setEdgeAction(1);
        BufferedImage effectImage = rippleFilter.filter(baseImage, null);
        effectImage = shadowFilter.filter(effectImage, null);
        graph.drawImage(effectImage, 0, 0, (Color) null, null);
        graph.dispose();
        return distortedImage;
    }
}

