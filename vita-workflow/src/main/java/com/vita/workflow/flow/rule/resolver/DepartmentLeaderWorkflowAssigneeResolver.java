package com.vita.workflow.flow.rule.resolver;

import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.system.sysDept.entity.SysDept;
import com.vita.system.sysDept.service.ISysDeptService;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import com.vita.workflow.api.WorkflowAssigneeResolver;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.flow.rule.resolver
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 部门负责人办理人解析器
 * @Version: 1.0
 */
@Component
@ConditionalOnWorkflowEnabled
public class DepartmentLeaderWorkflowAssigneeResolver implements WorkflowAssigneeResolver {

    public static final String RESOLVER_CODE = "DEPARTMENT_LEADER";

    private final ISysDeptService sysDeptService;

    private final ISysUserService sysUserService;

    public DepartmentLeaderWorkflowAssigneeResolver(
            ISysDeptService sysDeptService,
            ISysUserService sysUserService) {
        this.sysDeptService = sysDeptService;
        this.sysUserService = sysUserService;
    }

    /**
     * 获取解析器编码。
     *
     * @return 解析器编码
     */
    @Override
    public String code() {
        return RESOLVER_CODE;
    }

    /**
     * 根据 deptId 参数解析启用部门的负责人。
     *
     * @param arguments 规则参数
     * @return 部门负责人用户 ID
     */
    @Override
    public List<Long> resolve(Map<String, Object> arguments) {
        Long deptId = toLong(arguments.get("deptId"));
        if (deptId == null) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "部门负责人规则缺少deptId参数");
        }
        SysDept department = sysDeptService.getById(deptId);
        if (department == null
                || !CommonStatusEnum.isEnabled(department.getStatus())
                || department.getLeaderUserId() == null) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "部门未配置有效负责人");
        }
        SysUser leader = sysUserService.getById(department.getLeaderUserId());
        if (leader == null || !CommonStatusEnum.isEnabled(leader.getStatus())) {
            throw new ServiceException(
                    GlobalErrorCode.BAD_REQUEST.getCode(), "部门负责人不存在或已停用");
        }
        return List.of(department.getLeaderUserId());
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
