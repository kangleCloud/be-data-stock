package com.vita.app.auth.vo;

import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.vo
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: OAuth票据兑换响应
 * @Version: 1.0
 */
@Data
public class AppOAuthTicketExchangeVo {

    private String purpose;

    private String providerCode;

    private Boolean bound;

    private AppAuthTokenVo login;
}
