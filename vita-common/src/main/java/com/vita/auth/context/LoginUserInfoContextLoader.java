package com.vita.auth.context;

import com.vita.auth.model.LoginUserInfoModel;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.context
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 登录用户上下文加载器
 * @Version: 1.0
 */
public interface LoginUserInfoContextLoader {

    /**
     * 根据用户 ID 重建登录用户快照。
     *
     * @param userId 用户 ID
     * @return 登录用户快照；无法重建时返回 null
     */
    LoginUserInfoModel loadByUserId(Long userId);
}
