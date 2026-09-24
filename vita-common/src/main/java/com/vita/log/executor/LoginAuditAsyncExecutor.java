package com.vita.log.executor;

import com.vita.core.constant.Constants;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.log.model.LoginAuditRecord;
import com.vita.log.model.RequestClientInfo;
import com.vita.log.persistence.LoginAuditPersistence;
import com.vita.log.persistence.LoginLocationPersistence;
import com.vita.log.persistence.LoginUserInfoPersistence;
import com.vita.log.service.IpLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.executor
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 登录审计异步执行器
 * @Version: 1.0
 */
@Component
public class LoginAuditAsyncExecutor {

    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("sys-access");

    private static final Logger LOG = LoggerFactory.getLogger(LoginAuditAsyncExecutor.class);

    private final Executor defaultTaskExecutor;

    private final ObjectProvider<LoginAuditPersistence> loginAuditPersistenceProvider;

    private final ObjectProvider<LoginLocationPersistence> loginLocationPersistenceProvider;

    private final ObjectProvider<LoginUserInfoPersistence> loginUserInfoPersistenceProvider;

    private final IpLocationService ipLocationService;

    public LoginAuditAsyncExecutor(@Qualifier("defaultTaskExecutor") Executor defaultTaskExecutor,
                                   ObjectProvider<LoginAuditPersistence> loginAuditPersistenceProvider,
                                   ObjectProvider<LoginLocationPersistence> loginLocationPersistenceProvider,
                                   ObjectProvider<LoginUserInfoPersistence> loginUserInfoPersistenceProvider,
                                   IpLocationService ipLocationService) {
        this.defaultTaskExecutor = defaultTaskExecutor;
        this.loginAuditPersistenceProvider = loginAuditPersistenceProvider;
        this.loginLocationPersistenceProvider = loginLocationPersistenceProvider;
        this.loginUserInfoPersistenceProvider = loginUserInfoPersistenceProvider;
        this.ipLocationService = ipLocationService;
    }

    /**
     * 提交登录审计任务。
     *
     * @param requestId         请求ID
     * @param userName          用户名
     * @param userId            用户ID
     * @param requestClientInfo 请求客户端信息快照
     * @param status            登录状态
     * @param message           审计消息
     */
    public void submit(String requestId,
                       String userName,
                       Long userId,
                       RequestClientInfo requestClientInfo,
                       String status,
                       String message) {
        if (requestClientInfo == null) {
            return;
        }

        LoginAuditPersistence loginAuditPersistence = loginAuditPersistenceProvider.getIfAvailable();
        LoginLocationPersistence loginLocationPersistence = loginLocationPersistenceProvider.getIfAvailable();
        LoginUserInfoPersistence loginUserInfoPersistence = loginUserInfoPersistenceProvider.getIfAvailable();
        LoginAuditRecord loginAuditRecord = buildRecord(userName, userId, requestClientInfo, status, message);
        defaultTaskExecutor.execute(() -> persistLoginAudit(
                requestId,
                loginAuditRecord,
                loginAuditPersistence,
                loginLocationPersistence,
                loginUserInfoPersistence
        ));
    }

    private void persistLoginAudit(String requestId,
                                   LoginAuditRecord loginAuditRecord,
                                   LoginAuditPersistence loginAuditPersistence,
                                   LoginLocationPersistence loginLocationPersistence,
                                   LoginUserInfoPersistence loginUserInfoPersistence) {
        try {
            withRequestId(requestId);
            ACCESS_LOGGER.info(
                    "requestId={} event=login userName={} status={} ip={} location={} browser={} os={} msg={}",
                    requestId,
                    loginAuditRecord.getUserName(),
                    loginAuditRecord.getStatus(),
                    loginAuditRecord.getIpaddr(),
                    loginAuditRecord.getLoginLocation(),
                    loginAuditRecord.getBrowser(),
                    loginAuditRecord.getOs(),
                    loginAuditRecord.getMsg()
            );
            Long logId = loginAuditPersistence == null ? null : loginAuditPersistence.persist(loginAuditRecord);
            submitUserLoginInfoUpdate(requestId, loginAuditRecord, loginUserInfoPersistence);
            submitLocationEnrichment(requestId, loginAuditRecord, logId, loginLocationPersistence);
        } catch (Exception ex) {
            LOG.error("异步保存登录审计失败, requestId={}", requestId, ex);
        } finally {
            MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
        }
    }

    private void submitUserLoginInfoUpdate(String requestId,
                                           LoginAuditRecord loginAuditRecord,
                                           LoginUserInfoPersistence loginUserInfoPersistence) {
        if (loginUserInfoPersistence == null
                || loginAuditRecord.getUserId() == null
                || !Objects.equals(loginAuditRecord.getStatus(), Constants.SUCCESS)) {
            return;
        }
        defaultTaskExecutor.execute(() -> {
            try {
                withRequestId(requestId);
                loginUserInfoPersistence.updateLoginInfo(
                        loginAuditRecord.getUserId(),
                        loginAuditRecord.getLoginTime(),
                        loginAuditRecord.getIpaddr()
                );
            } catch (Exception ex) {
                LOG.error("异步更新用户登录信息失败, requestId={}, userId={}", requestId, loginAuditRecord.getUserId(), ex);
            } finally {
                MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
            }
        });
    }

    private void submitLocationEnrichment(String requestId,
                                          LoginAuditRecord loginAuditRecord,
                                          Long logId,
                                          LoginLocationPersistence loginLocationPersistence) {
        if (logId == null
                || loginLocationPersistence == null
                || !ipLocationService.needsAsyncEnrichment(loginAuditRecord.getIpaddr(), loginAuditRecord.getLoginLocation())) {
            return;
        }
        defaultTaskExecutor.execute(() -> {
            try {
                withRequestId(requestId);
                String location = ipLocationService.resolveAndCache(loginAuditRecord.getIpaddr());
                if (location != null && !Objects.equals(location, loginAuditRecord.getLoginLocation())) {
                    loginLocationPersistence.updateLoginLocation(logId, location);
                }
            } catch (Exception ex) {
                LOG.error("异步补全登录归属地失败, requestId={}, logId={}, ip={}", requestId, logId, loginAuditRecord.getIpaddr(), ex);
            } finally {
                MDC.remove(RequestTraceInterceptor.REQUEST_ID_KEY);
            }
        });
    }

    /**
     * 创建登录审计记录快照，避免异步执行期间请求信息被后续流程修改。
     *
     * @param userName          用户名
     * @param userId            用户ID
     * @param requestClientInfo 请求客户端信息
     * @param status            登录状态
     * @param message           审计消息
     * @return 登录审计快照
     */
    private LoginAuditRecord buildRecord(String userName,
                                         Long userId,
                                         RequestClientInfo requestClientInfo,
                                         String status,
                                         String message) {
        return LoginAuditRecord.builder()
                .userId(userId)
                .userName(userName)
                .ipaddr(requestClientInfo.getIp())
                .loginLocation(requestClientInfo.getLocation())
                .browser(requestClientInfo.getBrowser())
                .os(requestClientInfo.getOs())
                .status(status)
                .msg(message)
                .loginTime(requestClientInfo.getRequestTime())
                .build();
    }

    private void withRequestId(String requestId) {
        if (requestId != null) {
            MDC.put(RequestTraceInterceptor.REQUEST_ID_KEY, requestId);
        }
    }
}
