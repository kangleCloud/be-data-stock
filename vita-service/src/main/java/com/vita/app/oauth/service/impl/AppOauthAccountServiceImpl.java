package com.vita.app.oauth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.app.oauth.entity.AppOauthAccount;
import com.vita.app.oauth.mapper.AppOauthAccountMapper;
import com.vita.app.oauth.model.AppOAuthUserProfile;
import com.vita.app.oauth.service.IAppOauthAccountService;
import com.vita.app.user.service.IAppUserService;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.service.impl
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App OAuth账号绑定领域服务实现
 * @Version: 1.0
 */
@Service
public class AppOauthAccountServiceImpl
        extends ServiceImpl<AppOauthAccountMapper, AppOauthAccount>
        implements IAppOauthAccountService {

    private final AppOauthAccountMapper appOauthAccountMapper;
    private final IAppUserService appUserService;

    public AppOauthAccountServiceImpl(AppOauthAccountMapper appOauthAccountMapper,
                                      IAppUserService appUserService) {
        this.appOauthAccountMapper = appOauthAccountMapper;
        this.appUserService = appUserService;
    }

    /**
     * 绑定操作以两条全局唯一索引作为并发兜底，不允许平台账号或用户同平台绑定被静默覆盖。
     *
     * @param userId  App用户ID
     * @param profile OAuth公开资料
     * @return 绑定记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppOauthAccount bind(Long userId, AppOAuthUserProfile profile) {
        appUserService.getRequiredEnabledById(userId);
        normalizeAndValidate(profile);
        AppOauthAccount providerBinding = appOauthAccountMapper.selectByProviderIdentity(
                profile.getProviderCode(), profile.getProviderUserId());
        if (providerBinding != null) {
            if (!Objects.equals(providerBinding.getUserId(), userId)) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "该OAuth账号已绑定其他用户");
            }
            refreshProfile(providerBinding, profile, false);
            return providerBinding;
        }

        AppOauthAccount userProviderBinding = appOauthAccountMapper.selectByUserAndProvider(
                userId, profile.getProviderCode());
        if (userProviderBinding != null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "当前用户已绑定该OAuth平台");
        }

        AppOauthAccount binding = new AppOauthAccount();
        binding.setUserId(userId);
        binding.setProviderCode(profile.getProviderCode());
        binding.setProviderUserId(profile.getProviderUserId());
        binding.setBindTime(LocalDateTime.now());
        copyProfile(binding, profile);
        try {
            appOauthAccountMapper.insert(binding);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth账号绑定冲突");
        }
        return binding;
    }

    /**
     * OAuth登录只接受已存在绑定，不根据平台资料自动注册或按昵称、邮箱关联本地账号。
     *
     * @param profile OAuth公开资料
     * @return 已存在的绑定记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppOauthAccount authenticate(AppOAuthUserProfile profile) {
        normalizeAndValidate(profile);
        AppOauthAccount binding = appOauthAccountMapper.selectByProviderIdentity(
                profile.getProviderCode(), profile.getProviderUserId());
        if (binding == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), "OAuth账号尚未绑定");
        }
        appUserService.getRequiredEnabledById(binding.getUserId());
        refreshProfile(binding, profile, true);
        return binding;
    }

    @Override
    public List<String> getBoundProviderCodes(Long userId) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return appOauthAccountMapper.selectProviderCodesByUserId(userId);
    }

    private void refreshProfile(AppOauthAccount binding, AppOAuthUserProfile profile, boolean authenticated) {
        copyProfile(binding, profile);
        if (authenticated) {
            binding.setLastAuthTime(LocalDateTime.now());
        }
        appOauthAccountMapper.updateById(binding);
    }

    private void copyProfile(AppOauthAccount binding, AppOAuthUserProfile profile) {
        binding.setProviderLogin(profile.getProviderLogin());
        binding.setProviderNickName(profile.getProviderNickName());
        binding.setAvatarUrl(profile.getAvatarUrl());
        binding.setProfileUrl(profile.getProfileUrl());
    }

    private void normalizeAndValidate(AppOAuthUserProfile profile) {
        if (profile == null
                || CharSequenceUtil.isBlank(profile.getProviderCode())
                || CharSequenceUtil.isBlank(profile.getProviderUserId())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth用户资料不完整");
        }
        String providerCode = profile.getProviderCode().trim().toLowerCase(Locale.ROOT);
        String providerUserId = profile.getProviderUserId().trim();
        if (providerCode.length() > 32 || providerUserId.length() > 128) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth用户标识长度不合法");
        }
        profile.setProviderCode(providerCode);
        profile.setProviderUserId(providerUserId);
    }
}
