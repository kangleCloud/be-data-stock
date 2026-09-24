package com.vita.log.service;

import com.vita.log.executor.LoginAuditAsyncExecutor;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.log.model.RequestClientInfo;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.service
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 登录审计服务
 * @Version: 1.0
 */
@Component
public class LoginAuditService {

    private final LoginAuditAsyncExecutor loginAuditAsyncExecutor;

    public LoginAuditService(LoginAuditAsyncExecutor loginAuditAsyncExecutor) {
        this.loginAuditAsyncExecutor = loginAuditAsyncExecutor;
    }

    /**
     * 记录一次登录审计事件。
     *
     * @param userName          用户名
     * @param userId            用户ID
     * @param requestClientInfo 请求客户端信息
     * @param status            登录状态
     * @param message           审计消息
     */
    public void record(String userName,
                       Long userId,
                       RequestClientInfo requestClientInfo,
                       String status,
                       String message) {
        if (requestClientInfo == null) {
            return;
        }

        String requestId = MDC.get(RequestTraceInterceptor.REQUEST_ID_KEY);
        loginAuditAsyncExecutor.submit(requestId, userName, userId, requestClientInfo, status, message);
    }
}
