package com.vita.app.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.app.user.entity.AppUser;
import com.vita.app.user.mapper.AppUserMapper;
import com.vita.app.user.service.IAppUserService;
import com.vita.app.user.support.AppCredentialPolicy;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.crypto.Sm4Utils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.service.impl
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户认证领域服务实现
 * @Version: 1.0
 */
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements IAppUserService {

    private final AppUserMapper appUserMapper;

    public AppUserServiceImpl(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    /**
     * 注册App用户，用户名先规范化再依赖数据库唯一键完成最终并发兜底。
     *
     * @param userName 用户名
     * @param password 原始密码
     * @param nickName 用户昵称
     * @return 已创建用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppUser register(String userName, String password, String nickName) {
        String normalizedUserName = AppCredentialPolicy.normalizeAndValidateUserName(userName);
        AppCredentialPolicy.validatePassword(password);
        String normalizedNickName = AppCredentialPolicy.normalizeNickName(nickName, normalizedUserName);
        if (appUserMapper.selectByUserName(normalizedUserName) != null) {
            throw userNameExistsException();
        }

        AppUser user = new AppUser();
        user.setUserName(normalizedUserName);
        user.setPassword(Sm4Utils.encryptToBase64(password));
        user.setNickName(normalizedNickName);
        user.setStatus(CommonStatusEnum.ENABLED.getCode());
        user.setPwdUpdateDate(LocalDateTime.now());
        try {
            appUserMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw userNameExistsException();
        }
        return user;
    }

    /**
     * 密码登录始终使用统一失败文案，避免向外暴露用户名是否存在。
     *
     * @param userName 用户名
     * @param password 原始密码
     * @return 认证通过的用户
     */
    @Override
    public AppUser authenticate(String userName, String password) {
        String normalizedUserName;
        try {
            normalizedUserName = AppCredentialPolicy.normalizeAndValidateUserName(userName);
            AppCredentialPolicy.validatePassword(password);
        } catch (ServiceException ex) {
            throw loginFailureException();
        }

        AppUser user = appUserMapper.selectByUserName(normalizedUserName);
        if (user == null || !Objects.equals(Sm4Utils.encryptToBase64(password), user.getPassword())) {
            throw loginFailureException();
        }
        checkEnabled(user);
        return user;
    }

    /**
     * 查询并校验用户状态，OAuth登录与密码登录共用同一禁用规则。
     *
     * @param userId 用户ID
     * @return 启用用户
     */
    @Override
    public AppUser getRequiredEnabledById(Long userId) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        checkEnabled(user);
        return user;
    }

    /**
     * 登录成功后只更新登录审计快照，不修改用户名、密码或账号状态。
     *
     * @param userId       用户ID
     * @param loginAddress 登录地址
     */
    @Override
    public void recordLogin(Long userId, String loginAddress) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        AppUser update = new AppUser();
        update.setId(userId);
        update.setLoginTime(LocalDateTime.now());
        update.setLoginAddress(loginAddress);
        if (appUserMapper.updateById(update) != 1) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
    }

    private void checkEnabled(AppUser user) {
        if (CommonStatusEnum.isDisabled(user.getStatus())) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }
    }

    private ServiceException userNameExistsException() {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "用户名已存在");
    }

    private ServiceException loginFailureException() {
        return new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), AppCredentialPolicy.LOGIN_FAILURE_MESSAGE);
    }
}
