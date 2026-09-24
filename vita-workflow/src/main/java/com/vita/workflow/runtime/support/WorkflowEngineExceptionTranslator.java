package com.vita.workflow.runtime.support;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;

import java.util.function.Supplier;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.runtime.support
 * @Author: znk
 * @CreateTime: 2026-07-18
 * @Description: Warm-Flow 异常到 be-vita 业务异常的转换器
 * @Version: 1.0
 */
public final class WorkflowEngineExceptionTranslator {

    private WorkflowEngineExceptionTranslator() {
    }

    /**
     * 执行引擎操作并转换异常。
     *
     * @param action 引擎操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public static <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                throw new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION);
            }
            if (message.contains("权限") || message.contains("办理人")) {
                throw new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(), message);
            }
            if (message.contains("不存在") || message.contains("未找到")) {
                throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), message);
            }
            if (message.contains("结束") || message.contains("已办理") || message.contains("重复")) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
            }
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
        }
    }
}
