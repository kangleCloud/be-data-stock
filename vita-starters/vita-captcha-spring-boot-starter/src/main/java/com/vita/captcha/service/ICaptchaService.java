package com.vita.captcha.service;

import com.vita.captcha.dto.CaptchaRequestDto;
import com.vita.captcha.vo.CaptchaBasicResponseVo;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.captcha.service
 * @Author: znk
 * @CreateTime: 2026-03-20  21:35:47
 * @Description: 验证码服务接口，定义生成和验证验证码的方法
 * @Version: 1.0
 */
public interface ICaptchaService {

    /**
     * 获取验证码
     *
     * @param captchaRequestDto 验证码请求dto
     * @return
     */
    CaptchaBasicResponseVo getCaptcha(CaptchaRequestDto captchaRequestDto);

    /**
     * 校验验证码
     *
     * @param mix         混淆值，用户名登录必须为登录名，手机登录必须为手机号，其他为空
     * @param captchaCode 移动的坐标或验证码
     * @param encyptData  加密数据
     */
    void checkCaptcha(String mix, String captchaCode, String encyptData);
}
