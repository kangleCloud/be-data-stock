package com.vita.auth.config;

import com.vita.core.exception.ServiceException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestDispatchAuthInterceptorTest {

    @Test
    void authenticatesInitialRequestButNotAsyncOrErrorDispatch() throws Exception {
        HandlerInterceptor delegate = mock(HandlerInterceptor.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Object handler = new Object();
        when(delegate.preHandle(request, response, handler)).thenReturn(true);
        RequestDispatchAuthInterceptor interceptor = new RequestDispatchAuthInterceptor(delegate);

        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        when(request.getDispatcherType()).thenReturn(DispatcherType.ERROR);
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        when(request.getDispatcherType()).thenReturn(DispatcherType.FORWARD);
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();

        verify(delegate, times(2)).preHandle(request, response, handler);
    }

    @Test
    void preservesInitialAuthenticationAndPermissionFailures() throws Exception {
        HandlerInterceptor delegate = mock(HandlerInterceptor.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Object handler = new Object();
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        RequestDispatchAuthInterceptor interceptor = new RequestDispatchAuthInterceptor(delegate);

        for (int code : new int[]{401, 403}) {
            doThrow(new ServiceException(code, "鉴权失败"))
                    .when(delegate).preHandle(request, response, handler);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> interceptor.preHandle(request, response, handler));
            assertThat(exception.getCode()).isEqualTo(code);
        }
        verify(delegate, times(2)).preHandle(request, response, handler);
    }

    @Test
    void doesNotReauthenticateWhenSseCompletes() throws Exception {
        HandlerInterceptor delegate = mock(HandlerInterceptor.class);
        when(delegate.preHandle(any(), any(), any())).thenReturn(true);
        StreamProbeController controller = new StreamProbeController();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new RequestDispatchAuthInterceptor(delegate))
                .build();

        MvcResult initial = mvc.perform(get("/stream"))
                .andExpect(request().asyncStarted())
                .andReturn();
        controller.emitter.complete();
        mvc.perform(asyncDispatch(initial)).andExpect(status().isOk());

        verify(delegate, times(1)).preHandle(any(), any(), any());
    }

    @RestController
    private static final class StreamProbeController {
        private SseEmitter emitter;

        @GetMapping("/stream")
        public SseEmitter stream() {
            emitter = new SseEmitter();
            return emitter;
        }
    }
}
