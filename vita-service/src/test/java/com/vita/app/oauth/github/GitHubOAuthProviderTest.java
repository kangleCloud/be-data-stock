package com.vita.app.oauth.github;

import com.vita.app.oauth.github.property.GitHubOAuthProperty;
import com.vita.app.oauth.model.AppOAuthUserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.github
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: GitHub OAuth固定响应夹具测试
 * @Version: 1.0
 */
class GitHubOAuthProviderTest {

    private MockRestServiceServer server;
    private GitHubOAuthProvider provider;
    private AtomicReference<String> tokenRequest;

    @BeforeEach
    void setUp() {
        tokenRequest = new AtomicReference<>();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new GitHubOAuthProvider(property(), builder);
    }

    @Test
    void shouldExchangePublicProfileWithoutRequestingEmailScope() {
        server.expect(requestTo("https://github.example/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> tokenRequest.set(((MockClientHttpRequest) request).getBodyAsString()))
                .andRespond(withSuccess(
                        "{\"access_token\":\"provider-secret-token\",\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.example/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer provider-secret-token"))
                .andRespond(withSuccess(
                        "{\"id\":12345,\"login\":\"vita-user\",\"name\":\"Vita User\","
                                + "\"avatar_url\":\"https://img.example/avatar.png\","
                                + "\"html_url\":\"https://github.com/vita-user\"}",
                        MediaType.APPLICATION_JSON));

        String authorizationUrl = provider.buildAuthorizationUrl("state-value", "challenge-value");
        AppOAuthUserProfile profile = provider.exchangeUserProfile("oauth-code", "verifier-value");

        assertThat(authorizationUrl)
                .contains("code_challenge=challenge-value", "code_challenge_method=S256")
                .doesNotContain("scope=", "user%3Aemail");
        assertThat(tokenRequest.get())
                .contains("code=oauth-code", "code_verifier=verifier-value", "client_secret=client-secret");
        assertThat(profile.getProviderCode()).isEqualTo("github");
        assertThat(profile.getProviderUserId()).isEqualTo("12345");
        assertThat(profile.toString()).doesNotContain("provider-secret-token");
        server.verify();
    }

    private GitHubOAuthProperty property() {
        GitHubOAuthProperty property = new GitHubOAuthProperty();
        property.setEnabled(true);
        property.setClientId("client-id");
        property.setClientSecret("client-secret");
        property.setCallbackUri("https://app.example.com/app/api/auth/oauth/github/callback");
        property.setAuthorizationUri("https://github.example/authorize");
        property.setTokenUri("https://github.example/token");
        property.setUserUri("https://api.github.example/user");
        property.setConnectTimeout(Duration.ofSeconds(1));
        property.setReadTimeout(Duration.ofSeconds(1));
        return property;
    }

}
