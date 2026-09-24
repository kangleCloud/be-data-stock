package com.vita.captcha.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.captcha.config.property.CaptchaProperty;
import com.vita.captcha.common.enums.CaptchaTypeEnum;
import com.vita.captcha.dto.CaptchaRequestDto;
import com.vita.captcha.dto.CaptchaVerifyRequestDto;
import com.vita.captcha.utils.CaptchaUtils;
import com.vita.captcha.vo.CaptchaBasicResponseVo;
import com.vita.captcha.vo.CaptchaResponseVo;
import com.vita.captcha.vo.SlideCaptchaResponseVo;
import com.vita.core.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.service
 * @Author: znk
 * @CreateTime: 2026-03-20  21:49:14
 * @Description: 验证码应用服务，提供生成和校验验证码的核心业务逻辑
 * @Version: 1.0
 */
public class CaptchaApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaApplicationService.class);

    /**
     * 验证码属性
     */
    private final CaptchaProperty captchaProperty;
    private final Map<String, ICaptchaService> captchaServiceMap;

    public CaptchaApplicationService(CaptchaProperty captchaProperty,
                                     Map<String, ICaptchaService> captchaServiceMap) {
        this.captchaProperty = captchaProperty;
        this.captchaServiceMap = captchaServiceMap;
    }

    /**
     * 获取验证码
     *
     * @return
     */
    public CaptchaResponseVo captcha() {
        logger.info("生成验证码");
        return getCaptcha(new CaptchaRequestDto());
    }

    /**
     * 获取验证码
     *
     * @param captchaRequestDto
     * @return
     */
    public CaptchaResponseVo getCaptcha(CaptchaRequestDto captchaRequestDto) {
        CaptchaUtils.checkCaptchaEnabled(captchaProperty);
        CaptchaRequestDto requestDto = captchaRequestDto == null ? new CaptchaRequestDto() : captchaRequestDto;
        captchaProperty.normalizeAndValidate();
        String uuid = CaptchaUtils.resolveUuid(requestDto.getUuid());
        String mix = CaptchaUtils.resolveMix(requestDto.getMix(), uuid);
        requestDto.setUuid(uuid);
        requestDto.setMix(mix);
        ICaptchaService captchaService = getCurrentCaptchaService();
        logger.info("生成验证码，类型：{}", captchaProperty.getCaptchaType());
        CaptchaBasicResponseVo captchaBasicResponseVo = captchaService.getCaptcha(requestDto);
        if (CharSequenceUtil.isBlank(requestDto.getCaptchaCode())) {
            throw new ServiceException("验证码生成失败");
        }
        CaptchaResponseVo captchaResponseVo = new CaptchaResponseVo();
        captchaResponseVo.setUuid(uuid);
        captchaResponseVo.setCaptchaType(captchaProperty.getCaptchaType());
        captchaResponseVo.setEncryptData(CaptchaUtils.aesEncode(mix, requestDto.getCaptchaCode(), captchaProperty));
        captchaResponseVo.setCaptchaImg(captchaBasicResponseVo.getCaptchaImg());
        if (captchaBasicResponseVo instanceof SlideCaptchaResponseVo) {
            captchaResponseVo.setCaptchaData(captchaBasicResponseVo);
        }
        return captchaResponseVo;
    }

    /**
     * 校验验证码。
     *
     * @param captchaVerifyRequest 校验请求
     */
    public void checkCaptcha(CaptchaVerifyRequestDto captchaVerifyRequest) {
        CaptchaUtils.checkCaptchaEnabled(captchaProperty);
        if (captchaVerifyRequest == null) {
            throw new ServiceException("验证码校验参数不能为空");
        }
        String mix = CaptchaUtils.resolveMix(captchaVerifyRequest.getMix(), captchaVerifyRequest.getUuid());
        String captchaCode = captchaVerifyRequest.getCaptchaCode();
        String encryptData = captchaVerifyRequest.getEncryptData();
        if (CharSequenceUtil.isBlank(mix)
                || CharSequenceUtil.isBlank(captchaCode)
                || CharSequenceUtil.isBlank(encryptData)) {
            throw new ServiceException("验证码校验参数不能为空");
        }
        getCurrentCaptchaService().checkCaptcha(mix, captchaCode, encryptData);
    }

    private ICaptchaService getCurrentCaptchaService() {
        String serviceName = CaptchaTypeEnum.getService(captchaProperty.getCaptchaType());
        ICaptchaService captchaService = captchaServiceMap.get(serviceName);
        if (captchaService == null) {
            throw new ServiceException("当前验证码类型未启用:" + captchaProperty.getCaptchaType());
        }
        return captchaService;
    }
}
