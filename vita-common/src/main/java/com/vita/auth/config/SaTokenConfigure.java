package com.vita.auth.config;

import cn.dev33.satoken.fun.strategy.SaCorsHandleFunction;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import com.vita.auth.property.AuthProperty;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.log.interceptor.RequestTraceInterceptor;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.auth.config
 * @Author: znk
 * @CreateTime: 2026-03-07  14:01:49
 * @Description: Sa-Token配置类
 * @Version: 1.0
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SaTokenConfigure implements WebMvcConfigurer {

    @Resource
    private RequestTraceInterceptor requestTraceInterceptor;

    @Resource
    private AuthProperty authProperty;

    @Resource
    private StpLogic stpLogic;

    /**
     * 创建 Sa-Token 的跨域处理策略，并放行浏览器预检请求。
     *
     * @return 跨域处理策略
     */
    @Bean
    public SaCorsHandleFunction corsHandle() {
        return (req, res, sto) -> {
            String origin = req.getHeader("Origin");
            res
                    .setHeader("Access-Control-Allow-Origin", origin == null ? "*" : origin)
                    .setHeader("Access-Control-Allow-Credentials", "true")
                    .setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
                    .setHeader("Access-Control-Max-Age", "3600")
                    // 只保留一次，显式列出需要的头
                    .setHeader("Access-Control-Allow-Headers",
                            "Authorization, Content-Type, Accept, X-Requested-With, Cache-Control,Pragma")
                    .setHeader("Access-Control-Expose-Headers",
                            "Authorization");
            SaRouter.match(SaHttpMethod.OPTIONS).free(r -> {
                // 预检直接返回
            }).back();
        };
    }

    /**
     * 注册请求追踪和登录校验拦截器。
     * <p>
     * 登录校验使用当前应用注册的 {@link StpLogic}：App 模块使用 app 账号域，
     * 管理端使用默认 login 账号域，避免 App 请求落入 {@code StpUtil} 的管理端账号域。
     * 白名单路径跳过登录校验；异步和错误分派沿用首次请求的鉴权结果。
     *
     * @param registry Spring MVC 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTraceInterceptor)
                .addPathPatterns("/**");

        List<String> excludePaths = new ArrayList<>(authProperty.getExtraExcludePaths());
        // 所有业务模块共用登录校验，模块专属授权规则由各模块自行追加。
        registry.addInterceptor(new RequestDispatchAuthInterceptor(new SaInterceptor(handle -> {
                    try {
                        // 使用当前启动模块的账号域，避免 App 请求误用管理端默认 StpUtil。
                        stpLogic.checkLogin();
                    } catch (Exception e) {
                        throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
                    }

                })))
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);
    }
}
