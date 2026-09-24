package com.vita.app.auth.service;

import cn.dev33.satoken.stp.StpLogic;
import cn.hutool.core.text.CharSequenceUtil;
import com.vita.app.auth.dto.AppLoginDto;
import com.vita.app.auth.dto.AppRegisterDto;
import com.vita.app.auth.vo.AppAuthTokenVo;
import com.vita.app.auth.vo.AppUserProfileVo;
import com.vita.app.oauth.service.IAppOauthAccountService;
import com.vita.app.user.entity.AppUser;
import com.vita.app.user.service.IAppUserService;
import com.vita.captcha.dto.CaptchaVerifyRequestDto;
import com.vita.captcha.service.CaptchaApplicationService;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.resolver.RequestClientInfoResolver;
import org.springframework.stereotype.Service;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.service
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App认证应用服务
 * @Version: 1.0
 */
@Service
public class AppAuthApplicationService {

    private final IAppUserService appUserService;
    private final IAppOauthAccountService appOauthAccountService;
    private final CaptchaApplicationService captchaApplicationService;
    private final RequestClientInfoResolver requestClientInfoResolver;
    private final StpLogic stpLogic;

    public AppAuthApplicationService(IAppUserService appUserService,
                                     IAppOauthAccountService appOauthAccountService,
                                     CaptchaApplicationService captchaApplicationService,
                                     RequestClientInfoResolver requestClientInfoResolver,
                                     StpLogic stpLogic) {
        this.appUserService = appUserService;
        this.appOauthAccountService = appOauthAccountService;
        this.captchaApplicationService = captchaApplicationService;
        this.requestClientInfoResolver = requestClientInfoResolver;
        this.stpLogic = stpLogic;
    }

    /**
     * 注册只创建本地账号，不自动创建登录态，避免注册接口同时承担会话签发职责。
     *
     * @param request 注册请求
     * @return 已创建用户公开资料
     */
    public AppUserProfileVo register(AppRegisterDto request) {
        verifyCaptcha(request.getCaptchaCode(), request.getCaptchaUuid(),
                request.getCaptchaEncryptData(), request.getCaptchaMix());
        return buildProfile(appUserService.register(
                request.getUserName(), request.getPassword(), request.getNickName()));
    }

    /**
     * 用户名密码认证成功后签发App账号域令牌。
     *
     * @param request 登录请求
     * @return App登录令牌
     */
    public AppAuthTokenVo login(AppLoginDto request) {
        verifyCaptcha(request.getCaptchaCode(), request.getCaptchaUuid(),
                request.getCaptchaEncryptData(), request.getCaptchaMix());
        AppUser user = appUserService.authenticate(request.getUserName(), request.getPassword());
        return issueLogin(user);
    }

    /**
     * OAuth票据兑换成功后按本地用户ID签发令牌，仍需重新检查用户状态。
     *
     * @param userId App用户ID
     * @return App登录令牌
     */
    public AppAuthTokenVo loginByOAuth(Long userId) {
        return issueLogin(appUserService.getRequiredEnabledById(userId));
    }

    /**
     * 注销当前App登录态。
     */
    public void logout() {
        stpLogic.logout();
    }

    /**
     * 查询当前App用户资料及已绑定平台。
     *
     * @return 当前用户资料
     */
    public AppUserProfileVo getCurrentUser() {
        return buildProfile(appUserService.getRequiredEnabledById(getCurrentUserId()));
    }

    /**
     * 获取当前App账号域用户ID。
     *
     * @return App用户ID
     */
    public Long getCurrentUserId() {
        Object loginId = stpLogic.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException ex) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
    }

    private AppAuthTokenVo issueLogin(AppUser user) {
        RequestClientInfo clientInfo = requestClientInfoResolver.resolveCurrentRequest();
        // 先完成数据库登录快照更新，再签发令牌，避免数据库失败后仍向客户端返回有效会话。
        appUserService.recordLogin(user.getId(), clientInfo.getIp());
        stpLogic.login(user.getId());

        AppAuthTokenVo response = new AppAuthTokenVo();
        response.setTokenName(stpLogic.getTokenName());
        response.setTokenValue(stpLogic.getTokenValue());
        response.setTokenPrefix(stpLogic.getConfigOrGlobal().getTokenPrefix());
        response.setExpiresIn(stpLogic.getTokenInfo().getTokenTimeout());
        response.setUser(buildProfile(user));
        return response;
    }

    private AppUserProfileVo buildProfile(AppUser user) {
        AppUserProfileVo profile = new AppUserProfileVo();
        profile.setUserId(user.getId());
        profile.setUserName(user.getUserName());
        profile.setNickName(user.getNickName());
        profile.setAvatarUrl(user.getAvatarUrl());
        profile.setBoundProviders(appOauthAccountService.getBoundProviderCodes(user.getId()));
        return profile;
    }

    private void verifyCaptcha(String code, String uuid, String encryptData, String mix) {
        CaptchaVerifyRequestDto request = new CaptchaVerifyRequestDto();
        request.setCaptchaCode(code);
        request.setUuid(uuid);
        request.setEncryptData(encryptData);
        request.setMix(CharSequenceUtil.isBlank(mix) ? uuid : mix);
        captchaApplicationService.checkCaptcha(request);
    }
}
