package com.vita.test;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 测试中记录 SSE 事件内容。 */
public final class RecordingSseEmitter extends SseEmitter {
    private final List<String> events = new ArrayList<>();

    @Override
    public void send(SseEventBuilder builder) throws IOException {
        events.add(builder.build().stream().map(value -> String.valueOf(value.getData()))
                .reduce("", String::concat));
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}
