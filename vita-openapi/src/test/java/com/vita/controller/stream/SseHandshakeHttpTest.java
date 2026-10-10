package com.vita.controller.stream;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.ServiceException;
import com.vita.log.interceptor.RequestTraceInterceptor;
import com.vita.marketdata.constant.MarketDataConstants;
import com.vita.marketdata.etfmonitor.service.EtfMonitorStreamService;
import com.vita.marketdata.market.service.MarketSnapshotStreamService;
import com.vita.marketdata.stockmonitor.service.StockMonitorStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SseHttpTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.location=optional:classpath:/sse-handshake-test.yml",
                "server.servlet.context-path=/openapi/api", "logging.config=classpath:logback-console.xml",
                "spring.main.banner-mode=off"})
class SseHandshakeHttpTest {
    private static final String VERSION = "11111111111111111111111111111111";
    private final ObjectMapper json = new ObjectMapper();
    @LocalServerPort
    private int port;
    @Autowired
    private MarketSnapshotStreamService market;
    @Autowired
    private StockMonitorStreamService stocks;
    @Autowired
    private EtfMonitorStreamService etfs;
    @Autowired
    private StpLogic login;
    @Autowired
    private RequestTraceInterceptor trace;
    @Autowired
    private jakarta.servlet.ServletContext servletContext;

    @BeforeEach
    void prepareAnonymousRequest() throws Exception {
        assertEquals(1, servletContext.getFilterRegistrations().values().stream()
                .filter(filter -> filter.getClassName().equals("com.vita.web.xss.XssFilter")).count());
        reset(market, stocks, etfs, login, trace);
        when(trace.preHandle(any(), any(), any())).thenReturn(true);
        doThrow(new IllegalStateException("测试未登录")).when(login).checkLogin();
    }

    @ParameterizedTest
    @ValueSource(strings = {"market", "stock", "etf"})
    void anonymousHandshakeIsRealEventStreamWithReadyAndNormalCompletion(String kind) throws Exception {
        var emitter = new SseEmitter(MarketDataConstants.STREAM_TIMEOUT_MS);
        String field = "market".equals(kind) ? "snapshotId" : "stateId";
        emitter.send(SseEmitter.event().name("ready").data("{\"" + field + "\":\"" + VERSION + "\"}"));
        stubOpen(kind, () -> emitter);
        var connection = connect(path(kind), "GET", "text/event-stream");
        try {
            assertEquals(200, connection.getResponseCode());
            assertEquals("text/event-stream;charset=UTF-8", connection.getContentType());
            assertEquals("no-store", connection.getHeaderField("Cache-Control"));
            assertNull(connection.getHeaderField("Location"));
            try (var input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                assertEquals("event:ready", input.readLine());
                var payload = json.readTree(input.readLine().substring("data:".length()));
                assertEquals(VERSION, payload.path(field).asText());
                assertEquals(1, payload.size(), "首帧不包装 CommonResult");
                assertEquals("", input.readLine());
                // 以主动 complete 触发真实异步完成分派，不等待生产的 60 秒生命周期。
                emitter.complete();
                assertNull(input.readLine(), "正常完成为 EOF，不是 JSON 错误或登录响应");
            }
            verifyNoInteractions(login);
        } finally {
            emitter.complete();
            connection.disconnect();
        }
    }

    @ParameterizedTest
    @CsvSource({"market,404", "market,503", "stock,503", "etf,503", "market,401", "stock,401", "etf,401"})
    void handshakeServiceFailureKeepsHttpStatusAndJsonEvenWhenAcceptIsSse(String kind, int status) throws Exception {
        stubOpen(kind, () -> { throw new ServiceException(status, "测试握手失败"); });
        assertJsonError(kind, status, "text/event-stream");
    }

    @ParameterizedTest
    @ValueSource(strings = {"market", "stock", "etf"})
    void unexpectedHandshakeFailureMustBeHttp500Json(String kind) throws Exception {
        stubOpen(kind, () -> { throw new IllegalStateException("测试缓存连接失败"); });
        assertJsonError(kind, 500, "text/event-stream");
    }

    @Test
    void similarNonPublicPathStillRequiresLoginBeforeBusinessInvocation() throws Exception {
        var connection = connect("/market/dashboard/private", "GET", "application/json");
        try {
            int status = connection.getResponseCode();
            assertEquals(200, status, "普通接口沿用 CommonResult 业务状态约定");
            var error = json.readTree(connection.getInputStream());
            assertEquals(401, error.path("code").asInt());
            verify(login).checkLogin();
            verifyNoInteractions(market, stocks, etfs);
        } finally {
            connection.disconnect();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"market", "stock", "etf"})
    void streamOnlyAcceptsGet(String kind) throws Exception {
        var connection = connect(path(kind), "POST", "application/json");
        try {
            assertEquals(405, connection.getResponseCode());
            verifyNoInteractions(market, stocks, etfs);
        } finally {
            connection.disconnect();
        }
    }

    private void assertJsonError(String kind, int status, String accept) throws Exception {
        var connection = connect(path(kind), "GET", accept);
        try {
            assertEquals(status, connection.getResponseCode());
            assertTrue(connection.getContentType().startsWith("application/json"));
            assertEquals("no-store", connection.getHeaderField("Cache-Control"));
            try (var input = connection.getErrorStream()) {
                assertNotNull(input);
                var error = json.readTree(input);
                assertEquals(status, error.path("code").asInt());
                assertFalse(error.path("success").asBoolean());
                assertFalse(error.path("msg").asText().isBlank());
            }
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection connect(String path, String method, String accept) throws Exception {
        var connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/openapi/api" + path).openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", accept);
        return connection;
    }

    private String path(String kind) {
        return switch (kind) {
            case "market" -> "/market/dashboard/stream";
            case "stock" -> "/stock-monitor/v1/stream";
            case "etf" -> "/etf-monitor/v1/stream";
            default -> throw new IllegalArgumentException("未知看板");
        };
    }

    private void stubOpen(String kind, Supplier<SseEmitter> result) {
        switch (kind) {
            case "market" -> when(market.open()).thenAnswer(call -> result.get());
            case "stock" -> when(stocks.open()).thenAnswer(call -> result.get());
            case "etf" -> when(etfs.open()).thenAnswer(call -> result.get());
            default -> throw new IllegalArgumentException("未知看板");
        }
    }
}
