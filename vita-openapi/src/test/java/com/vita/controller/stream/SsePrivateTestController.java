package com.vita.controller.stream;

import com.vita.core.CommonStreamResult;
import com.vita.core.StreamEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 仅测试源中存在的受保护流，用于验证首次鉴权和异步完成分派。 */
@RestController
public class SsePrivateTestController {
    private SseEmitter emitter;

    @GetMapping("/sse-test/private")
    @StreamEndpoint
    public ResponseEntity<SseEmitter> stream() throws Exception {
        emitter = new SseEmitter(60_000L);
        emitter.send(SseEmitter.event().name("ready").data("{}"));
        return CommonStreamResult.success(emitter);
    }

    public void complete() {
        if (emitter != null) emitter.complete();
    }
}
