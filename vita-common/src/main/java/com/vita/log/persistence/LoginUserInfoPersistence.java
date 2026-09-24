package com.vita.log.persistence;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.persistence
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 用户登录信息持久化扩展接口
 * @Version: 1.0
 */
public interface LoginUserInfoPersistence {

    /**
     * 更新用户最近登录信息。
     *
     * @param userId       用户ID
     * @param loginTime    登录时间
     * @param loginAddress 登录IP
     */
    void updateLoginInfo(Long userId, LocalDateTime loginTime, String loginAddress);
}
