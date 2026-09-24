package com.vita.app.auth.vo;

import lombok.Data;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.auth.vo
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户公开资料响应
 * @Version: 1.0
 */
@Data
public class AppUserProfileVo {

    private Long userId;

    private String userName;

    private String nickName;

    private String avatarUrl;

    private List<String> boundProviders;
}
