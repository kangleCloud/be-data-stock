package com.vita.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.vita.auth.service.IAuthCommonService;
import com.vita.auth.service.IAuthTokenService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限接口实现。
 *
 * @author znk
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final IAuthCommonService authCommonService;
    private final IAuthTokenService authTokenService;

    public StpInterfaceImpl(IAuthCommonService authCommonService,
                            IAuthTokenService authTokenService) {
        this.authCommonService = authCommonService;
        this.authTokenService = authTokenService;
    }

    /**
     * sa-token 获取当前登录用户权限集合
     *
     * @param loginId   账号id
     * @param loginType 账号类型
     * @return 权限集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // App账号域不接入管理端RBAC，避免相同数字ID命中sys_user权限。
        if (!StpUtil.TYPE.equals(loginType)) {
            return CollUtil.newArrayList();
        }
        Long userId = resolveUserId(loginId);
        if (userId == null) {
            return CollUtil.newArrayList();
        }
        List<String> permissionCodes = castStringList(authTokenService.getStoredLoginPermissionCodes());
        if (CollUtil.isNotEmpty(permissionCodes)) {
            return permissionCodes;
        }
        permissionCodes = authCommonService.getPermissionCodesByUserId(userId);
        authTokenService.storeLoginPermissionCodes(permissionCodes);
        return permissionCodes;
    }

    /**
     * sa-token 获取当前登录用户角色集合
     *
     * @param loginId   账号id
     * @param loginType 账号类型
     * @return 角色集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 只有默认管理账号域允许回源sys_user角色。
        if (!StpUtil.TYPE.equals(loginType)) {
            return CollUtil.newArrayList();
        }
        Long userId = resolveUserId(loginId);
        if (userId == null) {
            return CollUtil.newArrayList();
        }
        List<String> roleCodes = castStringList(authTokenService.getStoredLoginRoleCodes());
        if (!roleCodes.isEmpty()) {
            return roleCodes;
        }
        roleCodes = authCommonService.getRoleCodesByUserId(userId);
        authTokenService.storeLoginRoleCodes(roleCodes);
        return roleCodes;
    }

    /**
     * 解析登录 ID，尝试转换为 Long 类型。
     *
     * @param loginId 登录 ID
     * @return 解析后的用户 ID，如果无法解析则返回 null
     */
    private Long resolveUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 将任意对象安全转换为 `List<String>`。
     * <p>
     * 仅当 `value` 为 `List` 时执行转换；否则返回空集合。
     * 转换过程中会忽略 `null` 元素，其余元素通过 `String.valueOf` 转为字符串。
     *
     * @param value 待转换对象
     * @return 转换后的字符串列表；若 `value` 不是列表则返回空列表
     */
    private List<String> castStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return CollUtil.newArrayList();
        }
        List<String> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }
}
