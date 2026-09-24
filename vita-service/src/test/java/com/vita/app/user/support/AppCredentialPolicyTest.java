package com.vita.app.user.support;

import com.vita.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.support
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App凭证规则测试
 * @Version: 1.0
 */
class AppCredentialPolicyTest {

    @Test
    void shouldNormalizeUserNameBeforePersistence() {
        assertThat(AppCredentialPolicy.normalizeAndValidateUserName("  Vita_User01  "))
                .isEqualTo("vita_user01");
    }

    @Test
    void shouldRejectUnsupportedUserNameCharacters() {
        assertThatThrownBy(() -> AppCredentialPolicy.normalizeAndValidateUserName("vita-user"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("小写字母、数字或下划线");
    }

    @Test
    void shouldUseUserNameWhenNickNameIsBlank() {
        assertThat(AppCredentialPolicy.normalizeNickName("  ", "vita_user"))
                .isEqualTo("vita_user");
    }
}
