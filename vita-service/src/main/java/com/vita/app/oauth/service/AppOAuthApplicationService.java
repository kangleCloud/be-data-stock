package com.vita.app.oauth.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.app.auth.service.AppAuthApplicationService;
import com.vita.app.auth.vo.AppOAuthAuthorizeVo;
import com.vita.app.auth.vo.AppOAuthTicketExchangeVo;
import com.vita.app.oauth.entity.AppOauthAccount;
import com.vita.app.oauth.model.AppOAuthPurpose;
import com.vita.app.oauth.model.AppOAuthState;
import com.vita.app.oauth.model.AppOAuthTicket;
import com.vita.app.oauth.model.AppOAuthUserProfile;
import com.vita.app.oauth.port.AppOAuthProvider;
import com.vita.app.oauth.property.AppOAuthProperty;
import com.vita.app.oauth.store.RedisAppOAuthFlowStore;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.service
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App OAuth授权、绑定和票据兑换应用服务
 * @Version: 1.0
 */
@Service
public class AppOAuthApplicationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, AppOAuthProvider> providers;
    private final RedisAppOAuthFlowStore flowStore;
    private final IAppOauthAccountService oauthAccountService;
    private final AppAuthApplicationService authApplicationService;
    private final AppOAuthProperty oauthProperty;

    public AppOAuthApplicationService(List<AppOAuthProvider> providers,
                                      RedisAppOAuthFlowStore flowStore,
                                      IAppOauthAccountService oauthAccountService,
                                      AppAuthApplicationService authApplicationService,
                                      AppOAuthProperty oauthProperty) {
        this.providers = buildProviderMap(providers);
        this.flowStore = flowStore;
        this.oauthAccountService = oauthAccountService;
        this.authApplicationService = authApplicationService;
        this.oauthProperty = oauthProperty;
    }

    /**
     * 创建匿名OAuth登录授权，回调只能登录已绑定账号，不能自动注册。
     *
     * @param providerCode 平台编码
     * @return 平台授权地址
     */
    public AppOAuthAuthorizeVo createLoginAuthorization(String providerCode) {
        return createAuthorization(providerCode, AppOAuthPurpose.LOGIN, null);
    }

    /**
     * 创建当前用户的OAuth绑定授权，用户ID仅保存在服务端state中，不下发到授权URL。
     *
     * @param providerCode 平台编码
     * @return 平台授权地址
     */
    public AppOAuthAuthorizeVo createBindAuthorization(String providerCode) {
        return createAuthorization(providerCode, AppOAuthPurpose.BIND, authApplicationService.getCurrentUserId());
    }

    /**
     * 消费state并完成平台资料交换；平台令牌不会进入返回值、Redis、数据库或日志。
     *
     * @param providerCode 回调平台编码
     * @param code         OAuth授权码
     * @param state        OAuth state
     * @param error        平台错误码
     * @return 固定前端地址及一次性ticket
     */
    public String handleCallback(String providerCode, String code, String state, String error) {
        if (CharSequenceUtil.isBlank(state)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth状态不能为空");
        }
        AppOAuthState oauthState = flowStore.consumeState(state);
        String normalizedProvider = normalizeProviderCode(providerCode);
        if (!normalizedProvider.equals(oauthState.getProviderCode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth平台与状态不匹配");
        }
        if (CharSequenceUtil.isNotBlank(error) || CharSequenceUtil.isBlank(code)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth授权未完成");
        }

        AppOAuthProvider provider = getRequiredProvider(normalizedProvider);
        AppOAuthUserProfile profile = provider.exchangeUserProfile(code, oauthState.getCodeVerifier());
        Long userId;
        if (oauthState.getPurpose() == AppOAuthPurpose.BIND) {
            AppOauthAccount binding = oauthAccountService.bind(oauthState.getUserId(), profile);
            userId = binding.getUserId();
        } else {
            AppOauthAccount binding = oauthAccountService.authenticate(profile);
            userId = binding.getUserId();
        }

        String ticketValue = randomUrlToken(32);
        AppOAuthTicket ticket = new AppOAuthTicket();
        ticket.setProviderCode(normalizedProvider);
        ticket.setPurpose(oauthState.getPurpose());
        ticket.setUserId(userId);
        flowStore.saveTicket(ticketValue, ticket, oauthProperty.getTicketTimeout());
        return buildFrontendRedirect("ticket", ticketValue);
    }

    /**
     * 原子消费ticket；只有LOGIN用途签发Sa-Token，BIND用途只返回绑定完成状态。
     *
     * @param ticketValue 一次性票据
     * @return 兑换结果
     */
    public AppOAuthTicketExchangeVo exchangeTicket(String ticketValue) {
        if (CharSequenceUtil.isBlank(ticketValue)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth票据不能为空");
        }
        AppOAuthTicket ticket = flowStore.consumeTicket(ticketValue.trim());
        AppOAuthTicketExchangeVo response = new AppOAuthTicketExchangeVo();
        response.setPurpose(ticket.getPurpose().name());
        response.setProviderCode(ticket.getProviderCode());
        response.setBound(ticket.getPurpose() == AppOAuthPurpose.BIND);
        if (ticket.getPurpose() == AppOAuthPurpose.LOGIN) {
            response.setLogin(authApplicationService.loginByOAuth(ticket.getUserId()));
        }
        return response;
    }

    /**
     * OAuth失败时只返回固定错误编码，不透传平台错误描述、授权码或state。
     *
     * @return 固定前端失败跳转地址
     */
    public String buildFailureRedirect() {
        return buildFrontendRedirect("error", "oauth_failed");
    }

    private AppOAuthAuthorizeVo createAuthorization(String providerCode, AppOAuthPurpose purpose, Long userId) {
        String normalizedProvider = normalizeProviderCode(providerCode);
        AppOAuthProvider provider = getRequiredProvider(normalizedProvider);
        String stateValue = randomUrlToken(32);
        String codeVerifier = randomUrlToken(64);
        String authorizationUrl = provider.buildAuthorizationUrl(stateValue, createCodeChallenge(codeVerifier));

        AppOAuthState state = new AppOAuthState();
        state.setProviderCode(normalizedProvider);
        state.setPurpose(purpose);
        state.setUserId(userId);
        state.setCodeVerifier(codeVerifier);
        flowStore.saveState(stateValue, state, oauthProperty.getStateTimeout());
        return new AppOAuthAuthorizeVo(authorizationUrl);
    }

    private String buildFrontendRedirect(String parameterName, String value) {
        String redirectUri = oauthProperty.getFrontendRedirectUri();
        validateAbsoluteUri(redirectUri, "OAuth前端回调地址未配置");
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam(parameterName, value)
                .build(true)
                .toUriString();
    }

    private String normalizeProviderCode(String providerCode) {
        if (CharSequenceUtil.isBlank(providerCode)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth平台编码不能为空");
        }
        return providerCode.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, AppOAuthProvider> buildProviderMap(List<AppOAuthProvider> providerList) {
        Map<String, AppOAuthProvider> providerMap = new HashMap<>();
        for (AppOAuthProvider provider : providerList) {
            String providerCode = normalizeProviderCode(provider.providerCode());
            if (providerMap.putIfAbsent(providerCode, provider) != null) {
                throw new IllegalStateException("OAuth平台适配器编码重复：" + providerCode);
            }
        }
        return Map.copyOf(providerMap);
    }

    private AppOAuthProvider getRequiredProvider(String providerCode) {
        AppOAuthProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth平台未启用");
        }
        return provider;
    }

    private String createCodeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), "当前JVM不支持SHA-256");
        }
    }

    private String randomUrlToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validateAbsoluteUri(String value, String message) {
        if (CharSequenceUtil.isBlank(value)) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
        }
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
        }
    }
}
