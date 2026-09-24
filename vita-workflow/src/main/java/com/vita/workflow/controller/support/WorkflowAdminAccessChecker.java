package com.vita.workflow.controller.support;

import cn.dev33.satoken.stp.StpUtil;
import com.vita.auth.constant.AuthConstants;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.controller.support
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 工作流管理端角色校验器
 * @Version: 1.0
 */
public final class WorkflowAdminAccessChecker {

    private WorkflowAdminAccessChecker() {
    }

    /**
     * 校验当前用户是否为超级管理员或子管理员。
     */
    public static void check() {
        if (StpUtil.hasRole(AuthConstants.SUPER_ADMIN_ROLE_CODE)
                || StpUtil.hasRole(AuthConstants.SUB_ADMIN_ROLE_CODE)) {
            return;
        }
        throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), "只有子管理员或超级管理员可以管理工作流");
    }
}
