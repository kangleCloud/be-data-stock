package com.vita.auth.service;

import com.vita.auth.dto.AuthLoginDto;
import com.vita.auth.vo.AuthInfoVo;
import com.vita.auth.vo.AuthLoginVo;
import com.vita.system.sysMenu.vo.RouterVo;

import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.service
 * @Author: Codex
 * @CreateTime: 2026-03-21
 * @Description: 认证服务接口
 * @Version: 1.0
 */
public interface IAuthService {

    /**
     * 执行管理端登录认证，返回令牌与当前登录用户快照。
     *
     * @param loginDto 登录请求参数
     * @return 登录响应
     */
    AuthLoginVo login(AuthLoginDto loginDto);

    /**
     * 注销当前登录用户的认证态。
     */
    void logout();

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前登录用户快照
     */
    AuthInfoVo getCurrentUserInfo();

    /**
     * 获取路由信息.
     */
    List<RouterVo> getRouters();
}
