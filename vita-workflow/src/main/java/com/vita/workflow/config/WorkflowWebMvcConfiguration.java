package com.vita.workflow.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.vita.auth.constant.AuthConstants;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Warm-Flow 内置接口认证配置。
 *
 * @author znk
 */
@Configuration
@ConditionalOnWorkflowEnabled
public class WorkflowWebMvcConfiguration implements WebMvcConfigurer {

    private static final List<String> WARM_FLOW_USER_PATHS = List.of(
            "/warm-flow/published-form",
            "/warm-flow/form-content",
            "/warm-flow/form-content/**",
            "/warm-flow/execute/load/**",
            "/warm-flow/execute/hisLoad/**",
            "/warm-flow/execute/handle"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局登录校验后，仅管理员可访问 Warm-Flow 设计和管理接口。
        registry.addInterceptor(new SaInterceptor(handle -> checkWorkflowAdministrator()))
                .addPathPatterns("/warm-flow/**")
                .excludePathPatterns(WARM_FLOW_USER_PATHS)
                .order(1);
    }

    private void checkWorkflowAdministrator() {
        if (StpUtil.hasRole(AuthConstants.SUPER_ADMIN_ROLE_CODE)
                || StpUtil.hasRole(AuthConstants.SUB_ADMIN_ROLE_CODE)) {
            return;
        }
        throw new ServiceException(GlobalErrorCode.FORBIDDEN);
    }
}
