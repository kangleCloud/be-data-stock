package com.vita.log.aspect;

import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.log.annotation.Log;
import com.vita.log.executor.OperLogAsyncExecutor;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.log.model.OperLogRecord;
import com.vita.log.support.LogUtils;
import com.vita.utils.web.ServletUtils;
import com.vita.utils.web.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.aspect
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 系统操作日志切面
 * @Version: 1.0
 */
@Aspect
@Component
public class LogAspect {

    private static final String[] EXCLUDE_PROPERTIES = {"password", "oldPassword", "newPassword", "confirmPassword"};

    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<>("sys-oper-log-cost");

    private final OperLogAsyncExecutor operLogAsyncExecutor;

    public LogAspect(OperLogAsyncExecutor operLogAsyncExecutor) {
        this.operLogAsyncExecutor = operLogAsyncExecutor;
    }

    /**
     * 控制器切点。
     */
    @Pointcut("execution(public * com.vita..controller..*Controller.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 日志注解切点。
     */
    @Pointcut("@annotation(com.vita.log.annotation.Log)")
    public void controllerLogPointcut() {
    }

    /**
     * 环绕采集操作日志信息。
     *
     * @param proceedingJoinPoint 切点信息
     * @param log                 日志注解
     * @return 目标方法返回结果
     * @throws Throwable 目标方法抛出的异常
     */
    @Around(value = "controllerPointcut() && @annotation(log)", argNames = "proceedingJoinPoint,log")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint, Log log) throws Throwable {
        long startTime = System.currentTimeMillis();
        TIME_THREADLOCAL.set(startTime);

        ServletRequestAttributes attributes = ServletUtils.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        Method method = resolveMethod(proceedingJoinPoint);
        LoginUserInfoModel loginUserInfo = LoginUserInfoModelContext.getLoginUserInfo();
        String requestParam = log.saveRequestData()
                ? LogUtils.toRequestLog(proceedingJoinPoint.getArgs(), EXCLUDE_PROPERTIES)
                : null;

        Object result = null;
        Throwable throwable = null;
        try {
            result = proceedingJoinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            OperLogRecord record = buildOperLogRecord(
                    log,
                    request,
                    method,
                    loginUserInfo,
                    requestParam,
                    result,
                    throwable,
                    System.currentTimeMillis() - startTime
            );
            operLogAsyncExecutor.submit(MDC.get(RequestTraceInterceptor.REQUEST_ID_KEY), record);
        }
    }

    /**
     * 清理日志切面线程上下文。
     */
    @After("controllerPointcut() && controllerLogPointcut()")
    public void clearThreadLocal() {
        TIME_THREADLOCAL.remove();
    }

    /**
     * 构建操作日志记录对象，并聚合当前请求上下文、用户信息、执行结果与耗时信息。
     *
     * @param log           日志注解，提供模块、业务类型及响应数据记录开关等配置
     * @param request       HTTP 请求对象，可能为 null（如非 Web 上下文）
     * @param method        当前被拦截的方法对象
     * @param loginUserInfo 当前登录用户信息，未登录或无法获取时为 null
     * @param requestParam  已序列化的请求参数日志内容
     * @param result        方法正常返回结果
     * @param throwable     方法执行异常，正常执行时为 null
     * @param costTime      方法执行耗时（毫秒）
     * @return 组装完成的操作日志记录对象
     */
    private OperLogRecord buildOperLogRecord(Log log,
                                             HttpServletRequest request,
                                             Method method,
                                             LoginUserInfoModel loginUserInfo,
                                             String requestParam,
                                             Object result,
                                             Throwable throwable,
                                             long costTime) {
        OperLogRecord record = new OperLogRecord();
        record.setModule(log.module());
        record.setBusinessType(log.businessType());
        record.setRequestMethod(request != null ? request.getMethod() : null);
        record.setOperUrl(request != null ? request.getRequestURL().toString() : null);
        record.setOperIp(request != null ? IpUtils.getIpAddr(request) : null);
        record.setOperLocation(null);
        record.setOperName(loginUserInfo != null ? loginUserInfo.getUsername() : null);
        record.setOperUserId(loginUserInfo != null ? loginUserInfo.getId() : null);
        record.setClassName(method.getDeclaringClass().getName());
        record.setMethodName(method.getName());
        record.setRequestParam(requestParam);
        record.setResponseResult(throwable == null && log.saveResponseData() ? LogUtils.toResponseLog(result) : null);
        record.setStatus(throwable == null ? (byte) 1 : (byte) 0);
        record.setErrorMsg(LogUtils.toErrorLog(throwable));
        record.setCostTime(costTime);
        record.setOperTime(LocalDateTime.now());
        return record;
    }

    /**
     * 解析被 AOP 代理的方法对象，优先返回目标类上的实际方法定义，
     * 以兼容 CGLIB 与 JDK 动态代理场景。
     *
     * @param proceedingJoinPoint 当前切点上下文
     * @return 目标方法对象；若目标类未找到同签名方法，则回退为签名中的方法
     */
    private Method resolveMethod(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = proceedingJoinPoint.getTarget().getClass();
        try {
            return targetClass.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            return method;
        }
    }
}
