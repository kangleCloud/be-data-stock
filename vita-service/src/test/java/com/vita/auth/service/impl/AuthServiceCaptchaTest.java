package com.vita.auth.service.impl;

import com.vita.auth.context.LoginUserInfoContextLoader;
import com.vita.auth.dto.AuthLoginDto;
import com.vita.auth.service.IAuthCommonService;
import com.vita.auth.service.IAuthTokenService;
import com.vita.captcha.dto.CaptchaVerifyRequestDto;
import com.vita.captcha.service.CaptchaApplicationService;
import com.vita.core.exception.ServiceException;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.resolver.RequestClientInfoResolver;
import com.vita.log.service.LoginAuditService;
import com.vita.system.sysIpBlock.service.ISysIpBlockService;
import com.vita.system.sysMenu.service.ISysMenuService;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 登录验证码参数与校验链路回归测试。
 *
 * @author znk
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceCaptchaTest {

    @Mock
    private ISysUserService sysUserService;
    @Mock
    private ISysIpBlockService sysIpBlockService;
    @Mock
    private IAuthCommonService authCommonService;
    @Mock
    private ObjectProvider<CaptchaApplicationService> captchaProvider;
    @Mock
    private CaptchaApplicationService captchaService;
    @Mock
    private IAuthTokenService authTokenService;
    @Mock
    private RequestClientInfoResolver requestClientInfoResolver;
    @Mock
    private LoginAuditService loginAuditService;
    @Mock
    private ISysMenuService sysMenuService;
    @Mock
    private LoginUserInfoContextLoader loginUserInfoContextLoader;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(captchaProvider.getIfAvailable()).thenReturn(captchaService);
        RequestClientInfo clientInfo = new RequestClientInfo();
        clientInfo.setIp("127.0.0.1");
        clientInfo.setRequestTime(LocalDateTime.now());
        when(requestClientInfoResolver.resolveCurrentRequest()).thenReturn(clientInfo);
        authService = new AuthServiceImpl(
                sysUserService,
                sysIpBlockService,
                authCommonService,
                captchaProvider,
                authTokenService,
                requestClientInfoResolver,
                loginAuditService,
                sysMenuService,
                loginUserInfoContextLoader
        );
    }

    @Test
    void enabledCaptchaShouldRejectMissingParameters() {
        AuthLoginDto loginDto = baseLogin();

        assertThatThrownBy(() -> authService.login(loginDto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("验证码参数不能为空");
    }

    @Test
    void captchaFailureShouldStopCredentialValidation() {
        AuthLoginDto loginDto = completeLogin();
        doThrow(new ServiceException("图形验证码错误"))
                .when(captchaService).checkCaptcha(any(CaptchaVerifyRequestDto.class));

        assertThatThrownBy(() -> authService.login(loginDto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("图形验证码错误");
    }

    @Test
    void completeCaptchaShouldReachCredentialValidation() {
        AuthLoginDto loginDto = completeLogin();
        when(sysUserService.getByUserName("admin")).thenReturn(null);

        assertThatThrownBy(() -> authService.login(loginDto))
                .isInstanceOf(ServiceException.class);

        ArgumentCaptor<CaptchaVerifyRequestDto> captor =
                ArgumentCaptor.forClass(CaptchaVerifyRequestDto.class);
        verify(captchaService).checkCaptcha(captor.capture());
        assertThat(captor.getValue().getUuid()).isEqualTo("captcha-uuid");
        assertThat(captor.getValue().getMix()).isEqualTo("captcha-uuid");
        assertThat(captor.getValue().getEncryptData()).isEqualTo("encrypted");
        verify(sysUserService).getByUserName("admin");
    }

    private AuthLoginDto baseLogin() {
        AuthLoginDto loginDto = new AuthLoginDto();
        loginDto.setUserName("admin");
        loginDto.setPassword("password123");
        return loginDto;
    }

    private AuthLoginDto completeLogin() {
        AuthLoginDto loginDto = baseLogin();
        loginDto.setCaptchaCode("ABCD");
        loginDto.setCaptchaUuid("captcha-uuid");
        loginDto.setCaptchaMix("captcha-uuid");
        loginDto.setCaptchaEncryptData("encrypted");
        return loginDto;
    }
}
