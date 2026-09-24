package com.vita.captcha.service;

import com.vita.captcha.common.enums.CaptchaTypeEnum;
import com.vita.captcha.config.property.CaptchaProperty;
import com.vita.captcha.dto.CaptchaRequestDto;
import com.vita.captcha.dto.SlideCaptchaRequestDto;
import com.vita.captcha.utils.CaptchaUtils;
import com.vita.captcha.utils.SlideCaptchaUtils;
import com.vita.captcha.vo.CaptchaBasicResponseVo;
import com.vita.captcha.vo.SlideCaptchaResponseVo;
import com.vita.core.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.service
 * @Author: znk
 * @CreateTime: 2026-03-20  22:09:04
 * @Description: 滑动验证码服务实现类，提供生成和校验滑动验证码的具体实现GraphCaptchaConfig
 * @Version: 1.0
 */
public class SlideCaptchaServiceImpl implements ICaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(SlideCaptchaServiceImpl.class);


    /**
     * 验证码属性
     */
    private final CaptchaProperty captchaProperty;

    public SlideCaptchaServiceImpl(CaptchaProperty captchaProperty) {
        this.captchaProperty = captchaProperty;
    }

    @Override
    public CaptchaBasicResponseVo getCaptcha(CaptchaRequestDto captchaRequestDto) {
        CaptchaUtils.checkSlideConfig(captchaProperty);
        if (captchaRequestDto == null) {
            throw new ServiceException("验证码请求不能为空");
        }
        CaptchaTypeEnum captchaTypeEnum = CaptchaTypeEnum.fromType(captchaProperty.getCaptchaType());
        if (!CaptchaTypeEnum.isSlideType(captchaTypeEnum.getType())) {
            throw new ServiceException("当前验证码类型不是滑动验证码");
        }
        SlideCaptchaRequestDto slideCaptchaRequestDto = captchaRequestDto.getSlideCaptcha();
        if (slideCaptchaRequestDto == null) {
            slideCaptchaRequestDto = new SlideCaptchaRequestDto();
            captchaRequestDto.setSlideCaptcha(slideCaptchaRequestDto);
        }
        SlideCaptchaUtils.checkCaptcha(captchaProperty, slideCaptchaRequestDto);
        SlideCaptchaUtils.BackgroundResource backgroundResource = SlideCaptchaUtils.resolveBackground(captchaProperty.getSlide());
        BufferedImage canvasImage = SlideCaptchaUtils.imageResize(backgroundResource.getBufferedImage(),
                slideCaptchaRequestDto.getCanvasWidth(),
                slideCaptchaRequestDto.getCanvasHeight());
        int blockX = slideCaptchaRequestDto.getBlockX() == null
                ? SlideCaptchaUtils.randomBlockX(slideCaptchaRequestDto)
                : slideCaptchaRequestDto.getBlockX();
        int blockY = SlideCaptchaUtils.randomBlockY(slideCaptchaRequestDto);
        BufferedImage blockImage = new BufferedImage(slideCaptchaRequestDto.getBlockWidth(),
                slideCaptchaRequestDto.getBlockHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);
        SlideCaptchaUtils.cutByTemplate(canvasImage,
                blockImage,
                slideCaptchaRequestDto.getBlockWidth(),
                slideCaptchaRequestDto.getBlockHeight(),
                slideCaptchaRequestDto.getBlockRadius(),
                blockX,
                blockY);
        captchaRequestDto.setCaptchaCode(String.valueOf(blockX));
        SlideCaptchaResponseVo slideCaptchaResponseVo = new SlideCaptchaResponseVo();
        slideCaptchaResponseVo.setCaptchaImg(CaptchaUtils.toBase64(canvasImage, "png"));
        slideCaptchaResponseVo.setBlockY(blockY);
        slideCaptchaResponseVo.setBackgroundUrl(backgroundResource.getAccessUrl());
        slideCaptchaResponseVo.setImgWidth(slideCaptchaRequestDto.getCanvasWidth());
        slideCaptchaResponseVo.setImgHeight(slideCaptchaRequestDto.getCanvasHeight());
        slideCaptchaResponseVo.setBlockWidth(slideCaptchaRequestDto.getBlockWidth());
        slideCaptchaResponseVo.setBlockHeight(slideCaptchaRequestDto.getBlockHeight());
        if (captchaTypeEnum == CaptchaTypeEnum.SLIDE) {
            slideCaptchaResponseVo.setBlockImg(CaptchaUtils.toBase64(blockImage, "png"));
        }
        return slideCaptchaResponseVo;
    }

    @Override
    public void checkCaptcha(String mix, String captchaCode, String encyptData) {
        CaptchaUtils.checkSlideConfig(captchaProperty);
        int sourceCode;
        int checkCode;
        try {
            sourceCode = Integer.parseInt(CaptchaUtils.aesDecrypt(mix, encyptData, captchaProperty));
            checkCode = Integer.parseInt(captchaCode);
        } catch (NumberFormatException ex) {
            logger.warn("滑动验证码校验失败，坐标格式错误", ex);
            throw new ServiceException("滑动验证码错误");
        }
        int deviation = Math.abs(sourceCode - checkCode);
        if (deviation > captchaProperty.getSlide().getDeviation()) {
            throw new ServiceException("滑动验证码错误");
        }
    }
}
