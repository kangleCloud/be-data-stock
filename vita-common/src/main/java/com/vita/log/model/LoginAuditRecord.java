package com.vita.log.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.model
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 登录审计记录模型
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginAuditRecord {

    /**
     * 登录用户ID，登录失败时可能为空。
     */
    private Long userId;

    /**
     * 登录用户名。
     */
    private String userName;

    /**
     * 登录来源 IP。
     */
    private String ipaddr;

    /**
     * 登录归属地。
     */
    private String loginLocation;

    /**
     * 浏览器名称。
     */
    private String browser;

    /**
     * 操作系统名称。
     */
    private String os;

    /**
     * 登录状态。
     */
    private String status;

    /**
     * 审计消息。
     */
    private String msg;

    /**
     * 登录时间。
     */
    private LocalDateTime loginTime;
}
