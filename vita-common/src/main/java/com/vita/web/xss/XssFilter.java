package com.vita.web.xss;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.CommonResult;
import com.vita.web.xss.property.XssProperty;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** 请求输入检查；响应、Token 请求头和第三方资料不经过本过滤器。 */
public final class XssFilter implements Filter {
    private static final Set<String> ADMIN_CREDENTIAL_PATHS = Set.of("/auth/login", "/system/sysUser/add", "/system/sysUser/update");
    private static final Set<String> APP_CREDENTIAL_PATHS = Set.of("/auth/login", "/auth/register");
    private final XssProperty property;
    private final ObjectMapper mapper;
    private final XssValidator validator;
    private final int maxBodySize;

    public XssFilter(XssProperty property, ObjectMapper mapper) {
        this.property = property;
        this.mapper = mapper;
        this.validator = new XssValidator(mapper.getFactory());
        long limit = property.getMaxJsonBodySize().toBytes();
        if (limit <= 0 || limit >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("XSS 文本检测大小必须为正数且小于 2GiB");
        }
        this.maxBodySize = (int) limit;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var http = (HttpServletRequest) request;
        var result = (HttpServletResponse) response;
        String path = http.getRequestURI().substring(http.getContextPath().length());
        if (!property.isEnabled() || http.getDispatcherType() != DispatcherType.REQUEST
                || "OPTIONS".equals(http.getMethod()) || property.getExcludePaths().contains(path)) {
            chain.doFilter(request, response);
            return;
        }
        MediaType type;
        try {
            type = http.getContentType() == null ? null : MediaType.parseMediaType(http.getContentType());
        } catch (IllegalArgumentException exception) {
            reject(result, 400, "请求内容格式不正确");
            return;
        }
        if (type != null && MediaType.MULTIPART_FORM_DATA.isCompatibleWith(type)) {
            // 沿用 Servlet 上传限制，仅读普通文本 part，绝不将文件二进制缓存到本过滤器。
            try {
                for (var part : http.getParts()) {
                    if (part.getSubmittedFileName() == null) {
                        byte[] value;
                        try (var input = part.getInputStream()) {
                            value = input.readNBytes(maxBodySize + 1);
                        }
                        if (value.length > maxBodySize) {
                            reject(result, 413, "请求文本超过允许大小");
                            return;
                        }
                        Charset charset = http.getCharacterEncoding() == null ? StandardCharsets.UTF_8
                                : Charset.forName(http.getCharacterEncoding());
                        if (part.getContentType() != null) {
                            Charset declared = MediaType.parseMediaType(part.getContentType()).getCharset();
                            if (declared != null) {
                                charset = declared;
                            }
                        }
                        if (!validator.isPlainText(part.getName())
                                || !validator.isPlainText(new String(value, charset))) {
                            reject(result, 400, "请求包含不允许的 HTML 内容");
                            return;
                        }
                    }
                }
            } catch (IllegalArgumentException exception) {
                reject(result, 400, "请求内容格式不正确");
                return;
            } catch (IllegalStateException exception) {
                reject(result, 413, "上传内容超过允许大小");
                return;
            }
        }
        for (var parameter : http.getParameterMap().entrySet()) {
            if (!validator.isPlainText(parameter.getKey())) {
                reject(result, 400, "请求包含不允许的 HTML 内容");
                return;
            }
            for (String value : parameter.getValue()) {
                if (!validator.isPlainText(value)) {
                    reject(result, 400, "请求包含不允许的 HTML 内容");
                    return;
                }
            }
        }
        if (type != null && "application".equalsIgnoreCase(type.getType())
                && ("json".equalsIgnoreCase(type.getSubtype()) || type.getSubtype().endsWith("+json"))) {
            if (http.getContentLengthLong() > maxBodySize) {
                reject(result, 413, "请求文本超过允许大小");
                return;
            }
            byte[] body = http.getInputStream().readNBytes(maxBodySize + 1);
            if (body.length > maxBodySize) {
                reject(result, 413, "请求文本超过允许大小");
                return;
            }
            try {
                if (!validator.isValidJson(body, isCredentialRequest(http, path))) {
                    reject(result, 400, "请求包含不允许的 HTML 内容");
                    return;
                }
            } catch (JsonProcessingException exception) {
                reject(result, 400, "请求内容格式不正确");
                return;
            }
            chain.doFilter(new XssRequestWrapper(http, body), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isCredentialRequest(HttpServletRequest request, String path) {
        // 现有凭据均为 POST JSON 顶层 password，查询、表单、嵌套字段和其他应用不豁免。
        return "POST".equals(request.getMethod())
                && ("/admin/api".equals(request.getContextPath()) && ADMIN_CREDENTIAL_PATHS.contains(path)
                || "/app/api".equals(request.getContextPath()) && APP_CREDENTIAL_PATHS.contains(path));
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        // Filter 异常不交给 ControllerAdvice，也不 sendError 触发容器 HTML 错误页。
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), CommonResult.error(status, message));
    }
}
