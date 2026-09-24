package com.vita.system.sysLoginLog.support;

import com.vita.log.persistence.LoginUserInfoPersistence;
import com.vita.system.sysUser.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 登录成功后用户最近登录信息异步更新适配器。
 *
 * @author znk
 */
@Component
public class SysUserLoginInfoPersistenceHandler implements LoginUserInfoPersistence {

    @Resource
    private ISysUserService sysUserService;

    @Override
    public void updateLoginInfo(Long userId, LocalDateTime loginTime, String loginAddress) {
        sysUserService.updateLoginInfo(userId, loginTime, loginAddress);
    }
}
