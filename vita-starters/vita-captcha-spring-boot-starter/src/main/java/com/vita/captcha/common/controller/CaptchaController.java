package com.vita.captcha.common.controller;

import com.vita.captcha.dto.CaptchaRequestDto;
import com.vita.captcha.dto.CaptchaVerifyRequestDto;
import com.vita.captcha.service.CaptchaApplicationService;
import com.vita.captcha.vo.CaptchaResponseVo;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.captcha.controller
 * @Author znk
 * @CreateTime 2026-03-20
 * @Description: 验证码 控制器
 */
@Validated
@RestController
@RequestMapping("/captcha")
@ConditionalOnProperty(prefix = "vita.captcha", name = "enabled", havingValue = "true")
public class CaptchaController extends BaseController {

    @Resource
    private CaptchaApplicationService captchaApplicationService;


    /**
     * 获取图形验证码
     *
     * @param captchaRequestDto 验证码请求dto
     */
    @PostMapping("/captcha")
    public CommonResult<CaptchaResponseVo> getCaptcha(@RequestBody @Validated CaptchaRequestDto captchaRequestDto) {
        return CommonResult.success(captchaApplicationService.getCaptcha(captchaRequestDto));
    }

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    public CommonResult<CaptchaResponseVo> captcha() {
        return CommonResult.success(captchaApplicationService.captcha());
    }

    /**
     * 校验验证码
     *
     * @param captchaVerifyRequest 验证码校验请求
     */
    @PostMapping("/check")
    public CommonResult<Boolean> checkCaptcha(@RequestBody @Validated CaptchaVerifyRequestDto captchaVerifyRequest) {
        captchaApplicationService.checkCaptcha(captchaVerifyRequest);
        return CommonResult.success(Boolean.TRUE);
    }
}
