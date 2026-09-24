package com.vita.app.oauth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.entity
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App OAuth账号绑定实体
 * @Version: 1.0
 */
@Data
@TableName("app_oauth_account")
@EqualsAndHashCode(callSuper = true)
public class AppOauthAccount extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("provider_code")
    private String providerCode;

    @TableField("provider_user_id")
    private String providerUserId;

    @TableField("provider_login")
    private String providerLogin;

    @TableField("provider_nick_name")
    private String providerNickName;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("profile_url")
    private String profileUrl;

    @TableField("bind_time")
    private LocalDateTime bindTime;

    @TableField("last_auth_time")
    private LocalDateTime lastAuthTime;

    @TableField("remark")
    private String remark;
}
