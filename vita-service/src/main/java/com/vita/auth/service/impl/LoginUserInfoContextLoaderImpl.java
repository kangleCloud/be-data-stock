package com.vita.auth.service.impl;

import com.vita.auth.constant.AuthConstants;
import com.vita.auth.context.LoginUserInfoContextLoader;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.auth.service.IAuthTokenService;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.resolver.RequestClientInfoResolver;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.springframework.stereotype.Service;

/**
 * 登录用户上下文加载器实现。
 */
@Service
public class LoginUserInfoContextLoaderImpl implements LoginUserInfoContextLoader {

    private final ISysUserService sysUserService;
    private final IAuthTokenService authTokenService;
    private final RequestClientInfoResolver requestClientInfoResolver;

    public LoginUserInfoContextLoaderImpl(ISysUserService sysUserService,
                                          IAuthTokenService authTokenService,
                                          RequestClientInfoResolver requestClientInfoResolver) {
        this.sysUserService = sysUserService;
        this.authTokenService = authTokenService;
        this.requestClientInfoResolver = requestClientInfoResolver;
    }

    /**
     * 根据用户 ID 重建登录用户快照。
     *
     * @param userId 用户 ID
     * @return 登录用户快照；无法重建时返回 null
     */
    @Override
    public LoginUserInfoModel loadByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return null;
        }
        return buildLoginUserInfo(user, requestClientInfoResolver.resolveCurrentRequest());
    }

    /**
     * 基于用户实体与当前请求客户端信息构建登录用户快照。
     *
     * @param user              用户实体
     * @param requestClientInfo 当前请求客户端信息
     * @return 登录用户快照
     */
    private LoginUserInfoModel buildLoginUserInfo(SysUser user, RequestClientInfo requestClientInfo) {
        LoginUserInfoModel loginUserInfo = new LoginUserInfoModel();
        loginUserInfo.setId(user.getId());
        loginUserInfo.setUsername(user.getUserName());
        loginUserInfo.setNickName(user.getNickName());
        loginUserInfo.setDeptId(user.getDeptId());
        loginUserInfo.setToken(authTokenService.getTokenValue());
        loginUserInfo.setTokenName(authTokenService.getTokenName());
        loginUserInfo.setTokenPrefix(authTokenService.getTokenPrefix());
        loginUserInfo.setAvatarUrl(user.getAvatarUrl());
        loginUserInfo.setIsSuperAdmin(user.getIsSuperAdmin());
        loginUserInfo.setIsSystem(user.getIsSystem());
        loginUserInfo.setLoginAddress(requestClientInfo.getIp());
        loginUserInfo.setLoginLocation(requestClientInfo.getLocation());
        loginUserInfo.setBrowser(requestClientInfo.getBrowser());
        loginUserInfo.setOs(requestClientInfo.getOs());
        loginUserInfo.setLoginTime(requestClientInfo.getRequestTime().format(AuthConstants.LOGIN_TIME_FORMATTER));
        return loginUserInfo;
    }
}
