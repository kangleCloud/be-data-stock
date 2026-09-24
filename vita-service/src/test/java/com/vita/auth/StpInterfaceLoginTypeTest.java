package com.vita.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: Sa-Token多账号域RBAC边界测试
 * @Version: 1.0
 */
class StpInterfaceLoginTypeTest {

    @Test
    void appLoginTypeShouldNeverLoadAdminRbac() {
        StpInterfaceImpl stpInterface = new StpInterfaceImpl(null, null);

        assertThat(stpInterface.getRoleList(1L, "app")).isEmpty();
        assertThat(stpInterface.getPermissionList(1L, "app")).isEmpty();
    }
}
