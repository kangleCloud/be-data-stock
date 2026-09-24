package com.vita.captcha.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.vo
 * @Author: znk
 * @CreateTime: 2026-03-20  21:41:28
 * @Description: 滑动验证码响应 VO，包含返回给前端的滑动验证码相关信息，继承自 CaptchaBasicResponseVo
 * @Version: 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SlideCaptchaResponseVo extends CaptchaBasicResponseVo {

    /**
     * 阻塞块的纵轴坐标
     **/
    private Integer blockY;
    /**
     * 生成的阻塞块的base64
     **/
    private String blockImg;
    /**
     * 背景图可访问网络地址。
     */
    private String backgroundUrl;
    /**
     * 背景图宽度。
     */
    private Integer imgWidth;
    /**
     * 背景图高度。
     */
    private Integer imgHeight;
    /**
     * 滑块宽度。
     */
    private Integer blockWidth;
    /**
     * 滑块高度。
     */
    private Integer blockHeight;

}
