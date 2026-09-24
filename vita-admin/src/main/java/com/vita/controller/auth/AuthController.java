package com.vita.controller.auth;

import com.vita.auth.dto.AuthLoginDto;
import com.vita.auth.service.IAuthService;
import com.vita.auth.vo.AuthInfoVo;
import com.vita.auth.vo.AuthLoginVo;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.system.sysMenu.vo.RouterVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.auth
 * @Author: Codex
 * @CreateTime: 2026-03-21
 * @Description: 认证控制器
 * @Version: 1.0
 */
@Validated
@RestController
@RequestMapping("/auth")
public class AuthController extends BaseController {

    private final IAuthService authService;


    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户认证接口
     *
     * @param loginDto 登录请求参数
     * @return 登录响应结果
     */
    @RepeatSubmit
    @PostMapping("/login")
    public CommonResult<AuthLoginVo> login(@RequestBody AuthLoginDto loginDto) {
        return CommonResult.success(authService.login(loginDto));
    }

    /**
     * 用户登出接口
     *
     * @return 登出结果
     */
    @RepeatSubmit
    @PostMapping("/logout")
    public CommonResult<Boolean> logout() {
        authService.logout();
        return CommonResult.success(Boolean.TRUE);
    }

    /**
     * 获取当前登录用户信息接口
     *
     * @return 当前登录用户信息
     */
    @GetMapping("/info")
    public CommonResult<AuthInfoVo> info() {
        return CommonResult.success(authService.getCurrentUserInfo());
    }

    /**
     * 获取路由信息
     */
    @GetMapping("/routers")
    public CommonResult<List<RouterVo>> getRouters() {
        return CommonResult.success(authService.getRouters());
    }

}
