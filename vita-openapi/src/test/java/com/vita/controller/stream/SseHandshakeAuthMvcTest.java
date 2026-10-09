package com.vita.controller.stream;

import cn.dev33.satoken.stp.StpLogic;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.marketdata.etfmonitor.service.EtfMonitorStreamService;
import com.vita.marketdata.market.service.MarketSnapshotStreamService;
import com.vita.marketdata.stockmonitor.service.StockMonitorStreamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 加载生产鉴权配置验证匿名精确路径与异步分派，不启动 HTTP 端口。 */
@SpringBootTest(classes = SseHttpTestConfiguration.class,
        properties = {"spring.config.location=optional:classpath:/sse-handshake-test.yml",
                "logging.config=classpath:logback-console.xml", "spring.main.banner-mode=off"})
@AutoConfigureMockMvc
class SseHandshakeAuthMvcTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private StpLogic login;
    @Autowired
    private RequestTraceInterceptor trace;
    @Autowired
    private MarketSnapshotStreamService market;
    @Autowired
    private StockMonitorStreamService stocks;
    @Autowired
    private EtfMonitorStreamService etfs;
    @Autowired
    private SsePrivateTestController privateStream;

    @BeforeEach
    void prepareRequest() throws Exception {
        reset(login, trace, market, stocks, etfs);
        when(trace.preHandle(any(), any(), any())).thenReturn(true);
    }

    @AfterEach
    void closePrivateStream() {
        privateStream.complete();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/market/dashboard/stream", "/stock-monitor/v1/stream", "/etf-monitor/v1/stream"})
    void exactPublicStreamRemainsAnonymous(String path) throws Exception {
        doThrow(new IllegalStateException("测试未登录")).when(login).checkLogin();
        var emitter = new SseEmitter(60_000L);
        emitter.send(SseEmitter.event().name("ready").data("{}"));
        when(market.open()).thenReturn(emitter);
        when(stocks.open()).thenReturn(emitter);
        when(etfs.open()).thenReturn(emitter);
        try {
            mvc.perform(get(path).accept("text/event-stream"))
                    .andExpect(status().isOk()).andExpect(request().asyncStarted())
                    .andExpect(header().string("Content-Type", "text/event-stream;charset=UTF-8"));
            verifyNoInteractions(login);
        } finally {
            emitter.complete();
        }
    }

    @Test
    void protectedStreamRejectsAnonymousHandshakeAsHttp401Json() throws Exception {
        doThrow(new IllegalStateException("测试未登录")).when(login).checkLogin();
        mvc.perform(get("/sse-test/private").accept("text/event-stream"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(401));
        verify(login).checkLogin();
    }

    @Test
    void asyncCompletionKeepsInitialAuthorizationInsteadOfCheckingLoginAgain() throws Exception {
        var result = mvc.perform(get("/sse-test/private").accept("text/event-stream"))
                .andExpect(status().isOk()).andExpect(request().asyncStarted()).andReturn();
        verify(login, times(1)).checkLogin();
        doThrow(new IllegalStateException("完成分派不得再次鉴权")).when(login).checkLogin();
        privateStream.complete();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/event-stream;charset=UTF-8"))
                .andExpect(content().string("event:ready\ndata:{}\n\n"));
        verify(login, times(1)).checkLogin();
    }
}
