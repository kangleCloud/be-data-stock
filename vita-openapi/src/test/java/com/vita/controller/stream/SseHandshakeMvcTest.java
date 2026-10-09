package com.vita.controller.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.auth.handler.SaTokenExceptionHandler;
import com.vita.controller.etfmonitor.EtfMonitorDashboardController;
import com.vita.controller.market.MarketDashboardController;
import com.vita.controller.stockmonitor.StockMonitorDashboardController;
import com.vita.core.exception.ControllerExceptionHandler;
import com.vita.core.exception.ExceptionMessageResolver;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.etfmonitor.service.EtfMonitorDashboardService;
import com.vita.marketdata.etfmonitor.service.EtfMonitorStreamService;
import com.vita.marketdata.market.service.MarketSnapshotService;
import com.vita.marketdata.market.service.MarketSnapshotStreamService;
import com.vita.marketdata.stockmonitor.service.StockMonitorService;
import com.vita.marketdata.stockmonitor.service.StockMonitorStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 无端口的 MVC 回归用于定位响应协商；真实容器握手另由 HTTP 测试覆盖。 */
class SseHandshakeMvcTest {
    private MockMvc mvc;
    private MarketSnapshotService snapshots;
    private MarketSnapshotStreamService market;
    private StockMonitorStreamService stocks;
    private EtfMonitorStreamService etfs;

    @BeforeEach
    void setupMvc() {
        market = mock(MarketSnapshotStreamService.class);
        snapshots = mock(MarketSnapshotService.class);
        stocks = mock(StockMonitorStreamService.class);
        etfs = mock(EtfMonitorStreamService.class);
        var resolver = new StaticListableBeanFactory().getBeanProvider(ExceptionMessageResolver.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new MarketDashboardController(snapshots, market),
                new StockMonitorDashboardController(mock(StockMonitorService.class), stocks),
                new EtfMonitorDashboardController(mock(EtfMonitorDashboardService.class), etfs))
                .setControllerAdvice(new ControllerExceptionHandler(resolver), new SaTokenExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"market", "stock", "etf"})
    void handshakeIsUnwrappedSse(String kind) throws Exception {
        var emitter = new SseEmitter(60_000L);
        String field = "market".equals(kind) ? "snapshotId" : "stateId";
        emitter.send(SseEmitter.event().name("ready").data("{\"" + field + "\":null}"));
        switch (kind) {
            case "market" -> when(market.open()).thenReturn(emitter);
            case "stock" -> when(stocks.open()).thenReturn(emitter);
            case "etf" -> when(etfs.open()).thenReturn(emitter);
            default -> throw new IllegalArgumentException("未知看板");
        }
        try {
            mvc.perform(get(path(kind)).accept("text/event-stream"))
                    .andExpect(status().isOk()).andExpect(request().asyncStarted())
                    .andExpect(header().string("Content-Type", "text/event-stream;charset=UTF-8"))
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().string("event:ready\ndata:{\"" + field + "\":null}\n\n"));
        } finally {
            emitter.complete();
        }
    }

    @ParameterizedTest
    @CsvSource({"market,404", "market,503", "stock,503", "etf,503"})
    void serviceFailureIsHttpJsonError(String kind, int status) throws Exception {
        failOpen(kind, new ServiceException(status, "测试握手失败"));
        mvc.perform(get(path(kind)).accept("text/event-stream"))
                .andExpect(status().is(status)).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(status)).andExpect(jsonPath("$.success").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"market", "stock", "etf"})
    void unexpectedFailureIsHttp500Json(String kind) throws Exception {
        failOpen(kind, new IllegalStateException("测试缓存连接失败"));
        mvc.perform(get(path(kind)).accept("text/event-stream"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(500)).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ordinaryRestUnexpectedFailureKeepsExistingCommonResultConvention() throws Exception {
        when(snapshots.getSnapshot()).thenThrow(new IllegalStateException("测试缓存连接失败"));
        mvc.perform(get("/market/dashboard/snapshot").accept("application/json"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(500)).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void moduleErrorInAvailableSnapshotDoesNotTurnHandshakeIntoJsonError() throws Exception {
        var json = new ObjectMapper();
        var cache = json.readTree("{\"snapshotId\":\"11111111111111111111111111111111\","
                + "\"modules\":{\"industrySectors\":{\"status\":\"ERROR\",\"data\":null}}}");
        when(snapshots.getSnapshot()).thenReturn(cache);
        var actualStream = new MarketSnapshotStreamService(snapshots, json);
        when(market.open()).thenAnswer(call -> actualStream.open());
        try {
            mvc.perform(get("/market/dashboard/snapshot").accept("application/json"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.content.modules.industrySectors.status").value("ERROR"));
            mvc.perform(get("/market/dashboard/stream").accept("text/event-stream"))
                    .andExpect(status().isOk()).andExpect(request().asyncStarted())
                    .andExpect(header().string("Content-Type", "text/event-stream;charset=UTF-8"))
                    .andExpect(content().string("event:ready\ndata:{\"snapshotId\":\"11111111111111111111111111111111\"}\n\n"));
        } finally {
            actualStream.shutdown();
        }
    }

    private void failOpen(String kind, RuntimeException exception) {
        switch (kind) {
            case "market" -> when(market.open()).thenThrow(exception);
            case "stock" -> when(stocks.open()).thenThrow(exception);
            case "etf" -> when(etfs.open()).thenThrow(exception);
            default -> throw new IllegalArgumentException("未知看板");
        }
    }

    private String path(String kind) {
        return switch (kind) {
            case "market" -> "/market/dashboard/stream";
            case "stock" -> "/stock-monitor/v1/stream";
            case "etf" -> "/etf-monitor/v1/stream";
            default -> throw new IllegalArgumentException("未知看板");
        };
    }
}
