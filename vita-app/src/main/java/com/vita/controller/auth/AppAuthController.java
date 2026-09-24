package com.vita.controller.auth;

import com.vita.app.auth.dto.AppLoginDto;
import com.vita.app.auth.dto.AppOAuthTicketExchangeDto;
import com.vita.app.auth.dto.AppRegisterDto;
import com.vita.app.auth.service.AppAuthApplicationService;
import com.vita.app.auth.vo.AppAuthTokenVo;
import com.vita.app.auth.vo.AppOAuthAuthorizeVo;
import com.vita.app.auth.vo.AppOAuthTicketExchangeVo;
import com.vita.app.auth.vo.AppUserProfileVo;
import com.vita.app.oauth.service.AppOAuthApplicationService;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.repeat.annotation.RepeatSubmit;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.controller.auth
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户认证与OAuth控制器
 * @Version: 1.0
 */
@Validated
@RestController
@RequestMapping("/auth")
public class AppAuthController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(AppAuthController.class);

    private final AppAuthApplicationService authApplicationService;
    private final AppOAuthApplicationService oauthApplicationService;

    public AppAuthController(AppAuthApplicationService authApplicationService,
                             AppOAuthApplicationService oauthApplicationService) {
        this.authApplicationService = authApplicationService;
        this.oauthApplicationService = oauthApplicationService;
    }

    /**
     * 注册本地App用户，成功后不自动登录。
     *
     * @param request 注册请求
     * @return 已创建用户资料
     */
    @RepeatSubmit
    @PostMapping("/register")
    public CommonResult<AppUserProfileVo> register(@RequestBody @Valid AppRegisterDto request) {
        return CommonResult.success(authApplicationService.register(request));
    }

    /**
     * 使用用户名、密码和验证码登录。
     *
     * @param request 登录请求
     * @return App令牌
     */
    @RepeatSubmit
    @PostMapping("/login")
    public CommonResult<AppAuthTokenVo> login(@RequestBody @Valid AppLoginDto request) {
        return CommonResult.success(authApplicationService.login(request));
    }

    /**
     * 注销当前App登录态。
     *
     * @return 注销结果
     */
    @RepeatSubmit
    @PostMapping("/logout")
    public CommonResult<Boolean> logout() {
        authApplicationService.logout();
        return CommonResult.success(Boolean.TRUE);
    }

    /**
     * 查询当前App用户资料。
     *
     * @return 当前用户资料
     */
    @GetMapping("/me")
    public CommonResult<AppUserProfileVo> me() {
        return CommonResult.success(authApplicationService.getCurrentUser());
    }

    /**
     * 获取匿名OAuth登录授权地址。
     *
     * @param providerCode OAuth平台编码
     * @return 授权地址
     */
    @GetMapping("/oauth/{providerCode}/authorize")
    public CommonResult<AppOAuthAuthorizeVo> oauthAuthorize(
            @PathVariable(name = "providerCode") String providerCode) {
        return CommonResult.success(oauthApplicationService.createLoginAuthorization(providerCode));
    }

    /**
     * 获取当前用户的OAuth绑定授权地址。
     *
     * @param providerCode OAuth平台编码
     * @return 授权地址
     */
    @GetMapping("/oauth/{providerCode}/bind-authorize")
    public CommonResult<AppOAuthAuthorizeVo> oauthBindAuthorize(
            @PathVariable(name = "providerCode") String providerCode) {
        return CommonResult.success(oauthApplicationService.createBindAuthorization(providerCode));
    }

    /**
     * 接收OAuth平台回调并跳转到固定前端地址；URL中只携带短期一次性票据。
     *
     * @param providerCode OAuth平台编码
     * @param code         OAuth授权码
     * @param state        OAuth state
     * @param error        OAuth平台错误码
     * @param response     Servlet响应
     * @throws IOException 跳转失败
     */
    @GetMapping("/oauth/{providerCode}/callback")
    public void oauthCallback(@PathVariable(name = "providerCode") String providerCode,
                              @RequestParam(name = "code", required = false) String code,
                              @RequestParam(name = "state", required = false) String state,
                              @RequestParam(name = "error", required = false) String error,
                              HttpServletResponse response) throws IOException {
        String redirectUri;
        try {
            redirectUri = oauthApplicationService.handleCallback(providerCode, code, state, error);
        } catch (Exception ex) {
            // 不记录code、state、平台令牌或客户端密钥，只保留平台和异常类型用于排查。
            logger.warn("OAuth回调失败，provider={}, type={}", providerCode, ex.getClass().getSimpleName());
            redirectUri = oauthApplicationService.buildFailureRedirect();
        }
        response.sendRedirect(redirectUri);
    }

    /**
     * 兑换一次性OAuth票据。
     *
     * @param request 票据请求
     * @return 登录或绑定结果
     */
    @RepeatSubmit
    @PostMapping("/oauth/ticket/exchange")
    public CommonResult<AppOAuthTicketExchangeVo> exchangeOAuthTicket(
            @RequestBody @Valid AppOAuthTicketExchangeDto request) {
        return CommonResult.success(oauthApplicationService.exchangeTicket(request.getTicket()));
    }
}
