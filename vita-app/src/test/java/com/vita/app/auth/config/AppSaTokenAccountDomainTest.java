package com.vita.app.auth.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import com.vita.auth.config.SaTokenSharedConfiguration;
import com.vita.auth.property.AuthProperty;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.config
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App Sa-Token账号域配置测试
 * @Version: 1.0
 */
class AppSaTokenAccountDomainTest {

    @Test
    void shouldCreateOnlyOneIndependentAppJwtLogic() {
        AuthProperty authProperty = new AuthProperty();
        authProperty.setLoginType("app");

        new ApplicationContextRunner()
                .withBean(AuthProperty.class, () -> authProperty)
                .withUserConfiguration(SaTokenSharedConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(StpLogic.class);
                    StpLogic stpLogic = context.getBean(StpLogic.class);
                    assertThat(stpLogic).isInstanceOf(StpLogicJwtForSimple.class);
                    assertThat(stpLogic.getLoginType()).isEqualTo("app");
                });
    }

    @Test
    void sharedLogicShouldKeepManagementLoginTypeByDefault() {
        AuthProperty authProperty = new AuthProperty();

        StpLogic stpLogic = new SaTokenSharedConfiguration().getStpLogicJwt(authProperty);

        assertThat(stpLogic.getLoginType()).isEqualTo("login");
    }
}
