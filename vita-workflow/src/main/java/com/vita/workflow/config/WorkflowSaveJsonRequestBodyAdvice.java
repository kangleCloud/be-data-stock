package com.vita.workflow.config;

import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.support.WorkflowDefinitionPermissionNormalizer;
import org.dromara.warm.flow.core.dto.DefJson;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * 拦截 Warm-Flow 首次保存请求，补齐人工节点默认管理员角色。
 *
 * @author Codex
 */
@RestControllerAdvice
@ConditionalOnWorkflowEnabled
public class WorkflowSaveJsonRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final WorkflowDefinitionPermissionNormalizer permissionNormalizer;

    public WorkflowSaveJsonRequestBodyAdvice(WorkflowDefinitionPermissionNormalizer permissionNormalizer) {
        this.permissionNormalizer = permissionNormalizer;
    }

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return targetType instanceof Class<?> clazz && DefJson.class.isAssignableFrom(clazz);
    }

    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (!(body instanceof DefJson defJson) || !isWarmFlowSaveJsonRequest()) {
            return body;
        }
        if (!permissionNormalizer.isFirstSave(defJson)) {
            return body;
        }
        return permissionNormalizer.normalizeInitialNodePermissions(defJson);
    }

    private boolean isWarmFlowSaveJsonRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return false;
        }
        String requestUri = attributes.getRequest().getRequestURI();
        String requestMethod = attributes.getRequest().getMethod();
        return HttpMethod.POST.matches(requestMethod)
                && requestUri != null
                && requestUri.endsWith(WorkflowConstants.WARM_FLOW_SAVE_JSON_PATH);
    }
}
