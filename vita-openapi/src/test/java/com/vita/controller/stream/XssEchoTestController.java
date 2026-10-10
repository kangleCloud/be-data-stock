package com.vita.controller.stream;

import com.vita.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** 仅测试源中的输入观察点，不调用数据库、缓存或真实登录服务。 */
@RestController
public class XssEchoTestController {
    private final AtomicInteger invocations = new AtomicInteger();

    @RequestMapping({"/xss-test/echo", "/xss-test/excluded", "/xss-test/excluded/child", "/outside"})
    public Map<String, Object> echo(HttpServletRequest request) throws Exception {
        invocations.incrementAndGet();
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("parameters", request.getParameterMap());
        result.put("token", request.getHeader("Authorization"));
        if (request.getContentType() != null && request.getContentType().startsWith("application/json")) {
            result.put("body", new String(request.getInputStream().readAllBytes(),StandardCharsets.UTF_8));
            result.put("reader", request.getReader().readLine());
        }
        if (request.getContentType() != null && request.getContentType().startsWith("multipart/")) {
            for (var part : request.getParts()) {
                if (part.getSubmittedFileName() != null) {
                    try (var input = part.getInputStream()) { result.put("file", new String(input.readAllBytes(),StandardCharsets.UTF_8)); }
                }
            }
        }
        return result;
    }

    @PostMapping({"/auth/login", "/auth/register"})
    public Map<String,Object> login(@RequestBody Map<String,Object> request) {
        invocations.incrementAndGet();
        return request;
    }

    @RequestMapping("/xss-test/forbidden")
    public void forbidden() {
        throw new ServiceException(403,"测试权限拒绝");
    }

    public int invocations() { return invocations.get(); }
    public void reset() { invocations.set(0); }
}
