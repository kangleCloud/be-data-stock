package com.vita.captcha.dto;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.dto
 * @Author: znk
 * @CreateTime: 2026-03-20  21:37:26
 * @Description: 滑动验证码请求参数 DTO，包含生成滑动验证码所需的参数
 * @Version: 1.0
 */
@Data
public class SlideCaptchaRequestDto {
    /**
     * 画布宽度
     **/
    private Integer canvasWidth;
    /**
     * 画布高度
     **/
    private Integer canvasHeight;
    /**
     * 阻塞块宽度
     **/
    private Integer blockWidth;
    /**
     * 阻塞块高度
     **/
    private Integer blockHeight;
    /**
     * 阻塞块凸凹半径
     **/
    private Integer blockRadius;
    /**
     * 阻塞块的横轴坐标
     **/
    private Integer blockX;
}
