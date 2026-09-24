package com.vita.app.oauth.port;

import com.vita.app.oauth.model.AppOAuthUserProfile;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.port
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: OAuth平台适配端口
 * @Version: 1.0
 */
public interface AppOAuthProvider {

    /**
     * 获取平台编码。
     *
     * @return 小写平台编码
     */
    String providerCode();

    /**
     * 构建固定回调地址的OAuth授权URL。
     *
     * @param state         防CSRF随机状态
     * @param codeChallenge PKCE S256挑战值
     * @return 平台授权URL
     */
    String buildAuthorizationUrl(String state, String codeChallenge);

    /**
     * 使用一次性授权码换取公开用户资料；平台令牌只能在实现方法内短暂使用。
     *
     * @param code         OAuth授权码
     * @param codeVerifier PKCE原始校验值
     * @return 平台公开用户资料
     */
    AppOAuthUserProfile exchangeUserProfile(String code, String codeVerifier);
}
