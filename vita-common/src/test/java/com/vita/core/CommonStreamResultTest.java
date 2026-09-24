package com.vita.core;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class CommonStreamResultTest {

    @Test
    void successUsesUncachedUtf8EventStream() {
        var response = CommonStreamResult.success(new SseEmitter());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("text/event-stream;charset=UTF-8");
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
    }

    @Test
    void errorWritesJsonWithRealHttpStatus() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        CommonStreamResult.error(response, 503, "市场快照格式不正确");
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":503")
                .contains("市场快照格式不正确");
    }
    @Test
    void streamRecognitionUsesControllerMarkerRatherThanAcceptHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/event-stream");
        request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,
                new HandlerMethod(new ProbeController(), "ordinary"));
        assertThat(CommonStreamResult.isStreamRequest(request)).isFalse();
        request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,
                new HandlerMethod(new ProbeController(), "stream"));
        assertThat(CommonStreamResult.isStreamRequest(request)).isTrue();
    }

    private static final class ProbeController {
        @StreamEndpoint
        public void stream() {
        }

        public void ordinary() {
        }
    }

}
