package com.vita.app.oauth.github;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vita.app.oauth.github.property.GitHubOAuthProperty;
import com.vita.app.oauth.model.AppOAuthUserProfile;
import com.vita.app.oauth.port.AppOAuthProvider;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.github
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: GitHub OAuth公开资料适配器
 * @Version: 1.0
 */
@Component
@ConditionalOnProperty(prefix = "vita.app.oauth.github", name = "enabled", havingValue = "true")
public class GitHubOAuthProvider implements AppOAuthProvider {

    private static final String PROVIDER_CODE = "github";
    private static final String GITHUB_API_VERSION = "2022-11-28";

    private final GitHubOAuthProperty property;
    private final RestClient restClient;

    public GitHubOAuthProvider(GitHubOAuthProperty property) {
        this(property, createRestClientBuilder(property));
    }

    GitHubOAuthProvider(GitHubOAuthProperty property, RestClient.Builder restClientBuilder) {
        validateProperty(property);
        this.property = property;
        this.restClient = restClientBuilder.build();
    }

    private static RestClient.Builder createRestClientBuilder(GitHubOAuthProperty property) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) property.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) property.getReadTimeout().toMillis());
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    /**
     * GitHub授权只请求公开身份，不传递user:email等额外scope。
     *
     * @param state         防CSRF随机状态
     * @param codeChallenge PKCE S256挑战值
     * @return GitHub授权地址
     */
    @Override
    public String buildAuthorizationUrl(String state, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(property.getAuthorizationUri())
                .queryParam("client_id", property.getClientId())
                .queryParam("redirect_uri", property.getCallbackUri())
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build(true)
                .toUriString();
    }

    /**
     * 平台access token只存在于当前方法局部变量，完成公开资料查询后立即丢弃。
     *
     * @param code         OAuth授权码
     * @param codeVerifier PKCE原始校验值
     * @return GitHub公开用户资料
     */
    @Override
    public AppOAuthUserProfile exchangeUserProfile(String code, String codeVerifier) {
        try {
            GitHubTokenResponse tokenResponse = exchangeToken(code, codeVerifier);
            if (tokenResponse == null || CharSequenceUtil.isBlank(tokenResponse.getAccessToken())) {
                throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), "GitHub授权失败");
            }
            GitHubUserResponse user = loadUser(tokenResponse.getAccessToken());
            if (user == null || user.getId() == null || CharSequenceUtil.isBlank(user.getLogin())) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "GitHub用户资料不完整");
            }
            AppOAuthUserProfile profile = new AppOAuthUserProfile();
            profile.setProviderCode(PROVIDER_CODE);
            profile.setProviderUserId(String.valueOf(user.getId()));
            profile.setProviderLogin(user.getLogin());
            profile.setProviderNickName(CharSequenceUtil.isBlank(user.getName()) ? user.getLogin() : user.getName());
            profile.setAvatarUrl(user.getAvatarUrl());
            profile.setProfileUrl(user.getHtmlUrl());
            return profile;
        } catch (HttpClientErrorException ex) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), "GitHub授权失败");
        } catch (RestClientException ex) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "GitHub OAuth服务不可用");
        }
    }

    private GitHubTokenResponse exchangeToken(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", property.getClientId());
        form.add("client_secret", property.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", property.getCallbackUri());
        form.add("code_verifier", codeVerifier);
        GitHubTokenResponse response = restClient.post()
                .uri(property.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .body(form)
                .retrieve()
                .body(GitHubTokenResponse.class);
        if (response != null && CharSequenceUtil.isNotBlank(response.getError())) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), "GitHub授权失败");
        }
        return response;
    }

    private GitHubUserResponse loadUser(String accessToken) {
        return restClient.get()
                .uri(property.getUserUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
                .retrieve()
                .body(GitHubUserResponse.class);
    }

    private void validateProperty(GitHubOAuthProperty value) {
        if (value == null
                || CharSequenceUtil.isBlank(value.getClientId())
                || CharSequenceUtil.isBlank(value.getClientSecret())
                || CharSequenceUtil.isBlank(value.getCallbackUri())) {
            throw new IllegalStateException("GitHub OAuth已启用但客户端配置不完整");
        }
        if (value.getConnectTimeout() == null || value.getConnectTimeout().isNegative()
                || value.getReadTimeout() == null || value.getReadTimeout().isNegative()) {
            throw new IllegalStateException("GitHub OAuth超时配置不合法");
        }
    }

    @Data
    private static class GitHubTokenResponse {

        @JsonProperty("access_token")
        private String accessToken;

        private String error;
    }

    @Data
    private static class GitHubUserResponse {

        private Long id;

        private String login;

        private String name;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        @JsonProperty("html_url")
        private String htmlUrl;
    }
}
