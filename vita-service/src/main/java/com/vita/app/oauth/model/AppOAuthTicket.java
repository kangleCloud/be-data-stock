package com.vita.app.oauth.model;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.model
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: Redis中的一次性OAuth结果票据
 * @Version: 1.0
 */
@Data
public class AppOAuthTicket {

    private String providerCode;

    private AppOAuthPurpose purpose;

    private Long userId;
}
