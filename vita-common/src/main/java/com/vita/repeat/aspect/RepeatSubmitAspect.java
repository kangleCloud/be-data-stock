package com.vita.repeat.aspect;

import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.redis.RedisCache;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.repeat.support.RepeatSubmitUtils;
import com.vita.utils.web.ServletUtils;
import com.vita.utils.web.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.repeat.aspect
 * @Author: znk
 * @CreateTime: 2026-06-17
 * @Description: 重复提交切面，按请求路径、身份与参数摘要进行短时间去重
 * @Version: 1.0
 */
@Aspect
@Component
@Order(1)
public class RepeatSubmitAspect {

    private final RedisCache redisCache;

    public RepeatSubmitAspect(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Pointcut("execution(public * com.vita..controller..*Controller.*(..))")
    public void controllerPointcut() {
    }

    @Around(value = "controllerPointcut() && @annotation(repeatSubmit)", argNames = "proceedingJoinPoint,repeatSubmit")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        ServletRequestAttributes attributes = ServletUtils.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        if (request == null) {
            return proceedingJoinPoint.proceed();
        }

        String identity = resolveIdentity(request);
        String argsDigest = RepeatSubmitUtils.digestArgs(proceedingJoinPoint.getArgs());
        String key = RepeatSubmitUtils.buildRepeatSubmitKey(request.getMethod(), request.getRequestURI(), identity, argsDigest);
        long timeout = repeatSubmit.interval();
        TimeUnit timeUnit = repeatSubmit.unit();
        if (timeout <= 0) {
            timeout = 1L;
        }

        boolean acquired = redisCache.setIfAbsent(key, Long.toString(System.currentTimeMillis()), timeout, timeUnit);
        if (!acquired) {
            throw new ServiceException(GlobalErrorCode.REPEATED_REQUESTS);
        }
        return proceedingJoinPoint.proceed();
    }

    private String resolveIdentity(HttpServletRequest request) {
        Long loginUserId = LoginUserInfoModelContext.getLoginUserId();
        if (loginUserId != null) {
            return String.valueOf(loginUserId);
        }
        String ip = IpUtils.getIpAddr(request);
        return (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) ? "anonymous" : ip;
    }
}
