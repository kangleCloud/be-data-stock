package com.vita.captcha.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.google.code.kaptcha.Producer;
import com.vita.captcha.common.enums.CaptchaTypeEnum;
import com.vita.captcha.config.property.CaptchaProperty;
import com.vita.captcha.dto.CaptchaRequestDto;
import com.vita.captcha.utils.CaptchaUtils;
import com.vita.captcha.vo.CaptchaBasicResponseVo;
import com.vita.core.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.service
 * @Author: znk
 * @CreateTime: 2026-03-20  22:08:23
 * @Description: 图形验证码服务实现类，提供生成和校验图形验证码的具体实现
 * @Version: 1.0
 */
public class GraphCaptchaServiceImpl implements ICaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(GraphCaptchaServiceImpl.class);

    /**
     * 验证码属性
     */
    private final CaptchaProperty captchaProperty;
    /**
     * 验证码生产者
     */
    private final Producer captchaProducer;
    /**
     * 计算验证码生产者
     */
    private final Producer captchaProducerMath;

    public GraphCaptchaServiceImpl(CaptchaProperty captchaProperty, Producer captchaProducer, Producer captchaProducerMath) {
        this.captchaProperty = captchaProperty;
        this.captchaProducer = captchaProducer;
        this.captchaProducerMath = captchaProducerMath;
    }

    @Override
    public CaptchaBasicResponseVo getCaptcha(CaptchaRequestDto captchaRequestDto) {
        CaptchaUtils.checkCaptchaEnabled(captchaProperty);
        if (captchaRequestDto == null) {
            throw new ServiceException("验证码请求不能为空");
        }
        CaptchaTypeEnum captchaTypeEnum = CaptchaTypeEnum.fromType(captchaProperty.getCaptchaType());
        if (!CaptchaTypeEnum.isGraphType(captchaTypeEnum.getType())) {
            throw new ServiceException("当前验证码类型不是图形验证码");
        }
        String captchaText;
        String captchaCode;
        BufferedImage image;
        if (captchaTypeEnum == CaptchaTypeEnum.MATH) {
            String text = captchaProducerMath.createText();
            int separator = text.lastIndexOf('@');
            if (separator < 0) {
                throw new ServiceException("图形答案验证码生成失败");
            }
            captchaText = text.substring(0, separator);
            captchaCode = text.substring(separator + 1);
            image = captchaProducerMath.createImage(captchaText);
        } else {
            captchaText = captchaProducer.createText();
            captchaCode = captchaText;
            image = captchaProducer.createImage(captchaText);
        }
        if (CharSequenceUtil.isBlank(captchaCode)) {
            throw new ServiceException("图形验证码生成失败");
        }
        captchaRequestDto.setCaptchaCode(captchaCode);
        CaptchaBasicResponseVo captchaBasicResponseVo = new CaptchaBasicResponseVo();
        captchaBasicResponseVo.setCaptchaImg(CaptchaUtils.toBase64(image, "jpg"));
        return captchaBasicResponseVo;
    }

    @Override
    public void checkCaptcha(String mix, String captchaCode, String encyptData) {
        CaptchaUtils.checkCaptchaEnabled(captchaProperty);
        String sourceCaptcha = CaptchaUtils.aesDecrypt(mix, encyptData, captchaProperty);
        if (CaptchaTypeEnum.fromType(captchaProperty.getCaptchaType()) == CaptchaTypeEnum.CHAR) {
            if (!Objects.equals(sourceCaptcha, captchaCode)) {
                logger.warn("字符验证码校验失败");
                throw new ServiceException("图形验证码错误");
            }
            return;
        }
        if (!Objects.equals(sourceCaptcha.trim(), captchaCode.trim())) {
            logger.warn("图形答案验证码校验失败");
            throw new ServiceException("图形验证码错误");
        }
    }
}
