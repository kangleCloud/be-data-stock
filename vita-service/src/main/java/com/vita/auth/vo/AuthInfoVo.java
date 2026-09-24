package com.vita.auth.vo;

import com.vita.auth.model.LoginUserInfoModel;
import lombok.Data;

import java.util.List;

/**
 * 登录后权限初始化响应。
 *
 * @author znk
 */
@Data
public class AuthInfoVo {

    /**
     * 当前用户基础信息。
     */
    private LoginUserInfoModel userInfo;

    /**
     * 当前用户角色码集合。
     */
    private List<String> roleCodes;

    /**
     * 当前用户权限码集合。
     */
    private List<String> permissionCodes;

}
