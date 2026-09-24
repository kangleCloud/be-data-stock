package com.vita.app.oauth.mapper;

import com.vita.app.oauth.entity.AppOauthAccount;
import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.mapper
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App OAuth账号绑定Mapper
 * @Version: 1.0
 */
@Mapper
public interface AppOauthAccountMapper extends BaseMapperX<AppOauthAccount> {

    default AppOauthAccount selectByProviderIdentity(String providerCode, String providerUserId) {
        return selectOne(new LambdaQueryWrapperX<AppOauthAccount>()
                .eq(AppOauthAccount::getProviderCode, providerCode)
                .eq(AppOauthAccount::getProviderUserId, providerUserId));
    }

    default AppOauthAccount selectByUserAndProvider(Long userId, String providerCode) {
        return selectOne(new LambdaQueryWrapperX<AppOauthAccount>()
                .eq(AppOauthAccount::getUserId, userId)
                .eq(AppOauthAccount::getProviderCode, providerCode));
    }

    /**
     * 只投影平台编码，避免应用层加载并丢弃完整绑定实体。
     *
     * @param userId App用户ID
     * @return 已绑定平台编码
     */
    default List<String> selectProviderCodesByUserId(Long userId) {
        return selectObjs(new LambdaQueryWrapperX<AppOauthAccount>()
                .select(AppOauthAccount::getProviderCode)
                .eq(AppOauthAccount::getUserId, userId));
    }
}
