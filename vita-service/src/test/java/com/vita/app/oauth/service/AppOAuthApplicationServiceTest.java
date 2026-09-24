package com.vita.app.oauth.service;

import com.vita.app.auth.service.AppAuthApplicationService;
import com.vita.app.auth.vo.AppAuthTokenVo;
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
import com.vita.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.service
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App OAuth流程编排测试
 * @Version: 1.0
 */
class AppOAuthApplicationServiceTest {

    private InMemoryFlowStore flowStore;
    private FakeAccountService accountService;
    private FakeAuthApplicationService authService;
    private AppOAuthApplicationService service;

    @BeforeEach
    void setUp() {
        flowStore = new InMemoryFlowStore();
        accountService = new FakeAccountService();
        authService = new FakeAuthApplicationService();
        AppOAuthProperty property = new AppOAuthProperty();
        property.setFrontendRedirectUri("https://app.example.com/oauth/callback");
        property.setStateTimeout(Duration.ofMinutes(5));
        property.setTicketTimeout(Duration.ofSeconds(60));
        service = new AppOAuthApplicationService(
                List.of(new FakeProvider()),
                flowStore,
                accountService,
                authService,
                property
        );
    }

    @Test
    void loginFlowShouldUsePkceAndOneTimeTicket() {
        AppOAuthAuthorizeVo authorize = service.createLoginAuthorization("GitHub");
        String state = authorize.getAuthorizationUrl().substring(authorize.getAuthorizationUrl().indexOf("state=") + 6,
                authorize.getAuthorizationUrl().indexOf("&challenge="));
        AppOAuthState savedState = flowStore.states.get(state);

        assertThat(savedState.getPurpose()).isEqualTo(AppOAuthPurpose.LOGIN);
        assertThat(savedState.getCodeVerifier()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(authorize.getAuthorizationUrl()).contains("challenge=");

        String redirect = service.handleCallback("github", "oauth-code", state, null);
        String ticket = redirect.substring(redirect.indexOf("ticket=") + 7);
        AppOAuthTicketExchangeVo exchanged = service.exchangeTicket(ticket);

        assertThat(exchanged.getPurpose()).isEqualTo("LOGIN");
        assertThat(exchanged.getLogin()).isNotNull();
        assertThat(authService.lastLoginUserId).isEqualTo(99L);
        assertThatThrownBy(() -> service.exchangeTicket(ticket)).isInstanceOf(ServiceException.class);
    }

    @Test
    void bindFlowShouldUseCurrentUserAndNotIssueLoginToken() {
        AppOAuthAuthorizeVo authorize = service.createBindAuthorization("github");
        String state = authorize.getAuthorizationUrl().substring(authorize.getAuthorizationUrl().indexOf("state=") + 6,
                authorize.getAuthorizationUrl().indexOf("&challenge="));
        String redirect = service.handleCallback("github", "oauth-code", state, null);
        String ticket = redirect.substring(redirect.indexOf("ticket=") + 7);

        AppOAuthTicketExchangeVo exchanged = service.exchangeTicket(ticket);

        assertThat(accountService.boundUserId).isEqualTo(7L);
        assertThat(exchanged.getPurpose()).isEqualTo("BIND");
        assertThat(exchanged.getBound()).isTrue();
        assertThat(exchanged.getLogin()).isNull();
    }

    private static class FakeProvider implements AppOAuthProvider {

        @Override
        public String providerCode() {
            return "github";
        }

        @Override
        public String buildAuthorizationUrl(String state, String codeChallenge) {
            return "https://github.example/authorize?state=" + state + "&challenge=" + codeChallenge;
        }

        @Override
        public AppOAuthUserProfile exchangeUserProfile(String code, String codeVerifier) {
            AppOAuthUserProfile profile = new AppOAuthUserProfile();
            profile.setProviderCode("github");
            profile.setProviderUserId("12345");
            profile.setProviderLogin("vita-user");
            return profile;
        }
    }

    private static class InMemoryFlowStore extends RedisAppOAuthFlowStore {

        private final Map<String, AppOAuthState> states = new HashMap<>();
        private final Map<String, AppOAuthTicket> tickets = new HashMap<>();

        private InMemoryFlowStore() {
            super(null);
        }

        @Override
        public void saveState(String state, AppOAuthState value, Duration timeout) {
            states.put(state, value);
        }

        @Override
        public AppOAuthState consumeState(String state) {
            AppOAuthState value = states.remove(state);
            if (value == null) {
                throw new ServiceException("OAuth state unavailable");
            }
            return value;
        }

        @Override
        public void saveTicket(String ticket, AppOAuthTicket value, Duration timeout) {
            tickets.put(ticket, value);
        }

        @Override
        public AppOAuthTicket consumeTicket(String ticket) {
            AppOAuthTicket value = tickets.remove(ticket);
            if (value == null) {
                throw new ServiceException("OAuth ticket unavailable");
            }
            return value;
        }
    }

    private static class FakeAccountService implements IAppOauthAccountService {

        private Long boundUserId;

        @Override
        public AppOauthAccount bind(Long userId, AppOAuthUserProfile profile) {
            boundUserId = userId;
            return binding(userId);
        }

        @Override
        public AppOauthAccount authenticate(AppOAuthUserProfile profile) {
            return binding(99L);
        }

        @Override
        public List<String> getBoundProviderCodes(Long userId) {
            return List.of("github");
        }

        private AppOauthAccount binding(Long userId) {
            AppOauthAccount account = new AppOauthAccount();
            account.setUserId(userId);
            return account;
        }
    }

    private static class FakeAuthApplicationService extends AppAuthApplicationService {

        private Long lastLoginUserId;

        private FakeAuthApplicationService() {
            super(null, null, null, null, null);
        }

        @Override
        public Long getCurrentUserId() {
            return 7L;
        }

        @Override
        public AppAuthTokenVo loginByOAuth(Long userId) {
            lastLoginUserId = userId;
            return new AppAuthTokenVo();
        }
    }
}
