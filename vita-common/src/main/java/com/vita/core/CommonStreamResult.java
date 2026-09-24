package com.vita.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 流接口响应约定：成功时返回 SSE，握手失败时保留 CommonResult 业务错误体及真实 HTTP 状态。
 */
public final class CommonStreamResult {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CommonStreamResult() {
    }

    public static ResponseEntity<SseEmitter> success(SseEmitter emitter) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("text/event-stream;charset=UTF-8"))
                .body(emitter);
    }

    public static boolean isStreamRequest(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        return handler instanceof HandlerMethod method
                && method.getMethod().isAnnotationPresent(StreamEndpoint.class);
    }

    public static ResponseEntity<Void> error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        OBJECT_MAPPER.writeValue(response.getWriter(), CommonResult.error(status, message));
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).build();
    }
}
