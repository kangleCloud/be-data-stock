package com.vita.app.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.dto
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: OAuth一次性票据兑换请求
 * @Version: 1.0
 */
@Data
public class AppOAuthTicketExchangeDto {

    @NotBlank(message = "OAuth票据不能为空")
    private String ticket;
}
