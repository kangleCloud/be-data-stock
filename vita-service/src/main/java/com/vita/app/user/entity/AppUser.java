package com.vita.app.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.entity
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户实体
 * @Version: 1.0
 */
@Data
@TableName("app_user")
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BaseEntity {

    @TableField("user_name")
    private String userName;

    @TableField("password")
    private String password;

    @TableField("nick_name")
    private String nickName;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("status")
    private Byte status;

    @TableField("login_time")
    private LocalDateTime loginTime;

    @TableField("login_address")
    private String loginAddress;

    @TableField("pwd_update_date")
    private LocalDateTime pwdUpdateDate;

    @TableField("remark")
    private String remark;
}
