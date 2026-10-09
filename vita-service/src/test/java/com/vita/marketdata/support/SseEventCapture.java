package com.vita.marketdata.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 捕获实际 SSE 编码后的事件，避免测试依赖连接状态内部字段。 */
public final class SseEventCapture {
    private SseEventCapture() {
    }

    public static List<JsonNode> attach(SseEmitter emitter, ObjectMapper json) throws Exception {
        List<JsonNode> events = new CopyOnWriteArrayList<>();
        doAnswer(call -> {
            SseEmitter.SseEventBuilder builder = call.getArgument(0);
            StringBuilder wire = new StringBuilder();
            builder.build().forEach(part -> wire.append(part.getData()));
            String text = wire.toString();
            int data = text.indexOf("data:");
            var frame = json.createObjectNode();
            frame.put("event", text.substring("event:".length(), text.indexOf('\n')));
            frame.set("data", json.readTree(text.substring(data + "data:".length()).trim()));
            events.add(frame);
            return null;
        }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        return events;
    }

    public static String id(int value) {
        return String.format("%032x", value);
    }

    public static Message message(JsonNode notice) {
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(notice.toString().getBytes(StandardCharsets.UTF_8));
        return message;
    }
}
