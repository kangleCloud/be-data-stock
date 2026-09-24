package com.vita.app.oauth.service;

import com.vita.app.oauth.entity.AppOauthAccount;
import com.vita.app.oauth.model.AppOAuthUserProfile;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.service
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App OAuth账号绑定领域服务
 * @Version: 1.0
 */
public interface IAppOauthAccountService {

    /**
     * 将OAuth账号绑定到已注册App用户，禁止覆盖其他用户或更换同平台身份。
     *
     * @param userId  App用户ID
     * @param profile OAuth公开资料
     * @return 绑定记录
     */
    AppOauthAccount bind(Long userId, AppOAuthUserProfile profile);

    /**
     * 使用OAuth公开身份查找绑定并刷新资料快照。
     *
     * @param profile OAuth公开资料
     * @return 已存在的绑定记录
     */
    AppOauthAccount authenticate(AppOAuthUserProfile profile);

    /**
     * 查询用户已绑定的平台编码。
     *
     * @param userId App用户ID
     * @return 平台编码集合
     */
    List<String> getBoundProviderCodes(Long userId);
}
