package com.vita.stockmonitor.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.impl.StockMonitorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StockMonitorQuoteContractTest {
    private static final String SYMBOL = "SH600000";
    private static final String TRADE_DATE = "2026-09-28";
    private static final String BASE = "{\"schemaVersion\":1,\"symbol\":\"SH600000\",\"source\":\"XQ\","
            + "\"sourceTime\":\"2026-09-28T14:30:00+08:00\","
            + "\"collectedAt\":\"2026-09-28T14:30:03+08:00\","
            + "\"tradeDate\":\"2026-09-28\",\"price\":10.2,\"changePercent\":1.23,"
            + "\"amount\":1234567.89,";

    private ValueOperations<String, String> values;
    private StockMonitorServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        StockMonitorProperty property = new StockMonitorProperty();
        property.setXqEnabled(true);
        service = new StockMonitorServiceImpl(null, null, null, redis, null, null,
                new ObjectMapper(), property);
    }

    @Test
    void oldV1QuoteKeepsPriceAndReturnsNullForNewFields() {
        when(values.get(anyString())).thenReturn(BASE + "\"status\":\"FRESH\"}");

        StockMonitorDtos.Quote quote = readQuote();

        assertThat(quote.status()).isEqualTo("FRESH");
        assertThat(quote.price()).isEqualByComparingTo("10.2");
        assertThat(quote.low()).isNull();
        assertThat(quote.high()).isNull();
        assertThat(quote.open()).isNull();
        assertThat(quote.limitUp()).isNull();
        assertThat(quote.limitDown()).isNull();
        assertThat(quote.averagePrice()).isNull();
        assertThat(quote.volume()).isNull();
        assertThat(quote.previousClose()).isNull();
    }

    @Test
    void newQuotePassesThroughAllNumericFields() {
        when(values.get(anyString())).thenReturn(BASE
                + "\"low\":10.05,\"high\":10.31,\"open\":10.11,\"limitUp\":11,"
                + "\"limitDown\":9,\"averagePrice\":10.18,\"volume\":121274,"
                + "\"previousClose\":10.08,\"status\":\"FRESH\"}");

        StockMonitorDtos.Quote quote = readQuote();

        assertThat(quote.low()).isEqualByComparingTo("10.05");
        assertThat(quote.high()).isEqualByComparingTo("10.31");
        assertThat(quote.open()).isEqualByComparingTo("10.11");
        assertThat(quote.limitUp()).isEqualByComparingTo("11");
        assertThat(quote.limitDown()).isEqualByComparingTo("9");
        assertThat(quote.averagePrice()).isEqualByComparingTo("10.18");
        assertThat(quote.volume()).isEqualByComparingTo("121274");
        assertThat(quote.previousClose()).isEqualByComparingTo("10.08");
    }

    @Test
    void malformedNumberAndErrorStatusDoNotPublishOldPrices() {
        when(values.get(anyString())).thenReturn(BASE + "\"low\":\"bad\",\"status\":\"FRESH\"}");
        assertEmptyError(readQuote());

        when(values.get(anyString())).thenReturn(BASE + "\"volume\":1e999,\"status\":\"FRESH\"}");
        assertEmptyError(readQuote());

        when(values.get(anyString())).thenReturn(BASE + "\"previousClose\":\"bad\",\"status\":\"FRESH\"}");
        assertEmptyError(readQuote());

        when(values.get(anyString())).thenReturn(BASE + "\"status\":\"ERROR\"}");
        assertEmptyError(readQuote());
    }

    @Test
    void disabledDashboardDoesNotReadStoredQuote() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:monitor:v1:enabled")).thenReturn(
                "[{\"symbol\":\"SH600000\",\"code\":\"600000\",\"name\":\"浦发银行\",\"market\":\"SH\"}]");
        StockMonitorServiceImpl disabled = new StockMonitorServiceImpl(null, null, null, redis,
                null, null, new ObjectMapper(), new StockMonitorProperty());

        StockMonitorDtos.Dashboard dashboard = disabled.dashboard();

        assertThat(dashboard.xqEnabled()).isFalse();
        assertThat(dashboard.stocks()).hasSize(1);
        assertThat(dashboard.stocks().get(0).quote().status()).isEqualTo("DISABLED");
        assertThat(dashboard.stocks().get(0).quote().price()).isNull();
        assertThat(dashboard.stocks().get(0).quote().volume()).isNull();
        assertThat(dashboard.stocks().get(0).series()).isEmpty();
        assertThat(dashboard.stocks().get(0).fundSeries()).isEmpty();
        assertThat(dashboard.stocks().get(0).dataStatus()).isEqualTo("DISABLED");
        verify(values, never()).get(startsWith("stock:monitor:v1:quote:"));
        verify(values, never()).get(startsWith("stock:monitor:v1:series:"));
        verify(values, never()).get(startsWith("stock:monitor:v1:fund-series:"));
    }

    @Test
    void olderTradeDateRetainsValidQuoteAsStale() {
        when(values.get(anyString())).thenReturn(BASE + "\"status\":\"FRESH\"}");

        StockMonitorDtos.Quote quote = ReflectionTestUtils.invokeMethod(service, "readQuote", SYMBOL, "2026-09-29");

        assertThat(quote.status()).isEqualTo("STALE");
        assertThat(quote.tradeDate()).isEqualTo(TRADE_DATE);
        assertThat(quote.price()).isEqualByComparingTo(new BigDecimal("10.2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void staleDashboardReadsSeriesForQuoteTradeDate() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(invocation -> switch ((String) invocation.getArgument(0)) {
            case "stock:monitor:v1:enabled" ->
                    "[{\"symbol\":\"SH600000\",\"code\":\"600000\",\"name\":\"浦发银行\",\"market\":\"SH\"}]";
            case "stock:monitor:v1:lastTradeDate" -> "2026-09-29";
            case "stock:monitor:v1:quote:SH600000" -> BASE + "\"status\":\"STALE\"}";
            case "stock:monitor:v1:series:2026-09-28:SH600000" ->
                    "[{\"time\":\"2026-09-28T14:30:00+08:00\",\"price\":10.2}]";
            default -> null;
        });
        StockMonitorProfileMapper profiles = mock(StockMonitorProfileMapper.class);
        when(profiles.selectList(any(SFunction.class), anyCollection())).thenReturn(java.util.List.of());
        StockMonitorProperty property = new StockMonitorProperty();
        property.setXqEnabled(true);
        StockMonitorServiceImpl dashboardService = new StockMonitorServiceImpl(null, null, profiles,
                redis, null, null, new ObjectMapper(), property);

        StockMonitorDtos.Stock stock = dashboardService.dashboard().stocks().get(0);

        assertThat(stock.quote().status()).isEqualTo("STALE");
        assertThat(stock.series()).hasSize(1);
        verify(values).get("stock:monitor:v1:series:2026-09-28:SH600000");
    }

    @Test
    void historicalCloseUsesActualQuoteDateAndStrictlyAfterThreeOClock() {
        String close = BASE.replace("14:30:00", "15:02:00") + "\"status\":\"FRESH\"}";
        StockMonitorDtos.Stock confirmed = dashboardFrom(Map.of(
                "stock:monitor:v1:quote:SH600000", close)).stocks().get(0);

        assertThat(confirmed.effectiveTradeDate()).isEqualTo(TRADE_DATE);
        assertThat(confirmed.dataStatus()).isEqualTo("HISTORICAL");
        assertThat(confirmed.closeConfirmed()).isTrue();

        String beforeClose = BASE.replace("14:30:00", "14:56:00") + "\"status\":\"FRESH\"}";
        StockMonitorDtos.Stock unconfirmed = dashboardFrom(Map.of(
                "stock:monitor:v1:quote:SH600000", beforeClose)).stocks().get(0);
        assertThat(unconfirmed.closeConfirmed()).isFalse();
    }

    @Test
    void fundOnlySelectsFundDateAndNeverMixesOtherDayPrice() {
        StockMonitorDtos.Stock stock = dashboardFrom(Map.of(
                "stock:market:v1:snapshot", "{\"modules\":{\"marketFundFlow\":{\"tradeDate\":\"2026-09-29\",\"data\":{}}}}",
                "stock:monitor:v1:fund-series:2026-09-29:SH600000",
                "[{\"collectedAt\":\"2026-09-29T10:00:00+08:00\",\"inflow\":100,\"outflow\":40,\"netAmount\":60}]",
                "stock:monitor:v1:series:2026-09-28:SH600000",
                "[{\"time\":\"2026-09-28T14:30:00+08:00\",\"price\":10.2}]")).stocks().get(0);

        assertThat(stock.effectiveTradeDate()).isEqualTo("2026-09-29");
        assertThat(stock.fundSeries()).hasSize(1);
        assertThat(stock.series()).isEmpty();
        assertThat(stock.dataStatus()).isEqualTo("HISTORICAL");
    }

    @Test
    void validPriceDateWinsAndFundGapRemainsEmpty() {
        StockMonitorDtos.Stock stock = dashboardFrom(Map.of(
                "stock:monitor:v1:quote:SH600000", BASE + "\"status\":\"FRESH\"}",
                "stock:market:v1:snapshot", "{\"modules\":{\"marketFundFlow\":{\"tradeDate\":\"2026-09-29\",\"data\":{}}}}",
                "stock:monitor:v1:fund-series:2026-09-29:SH600000",
                "[{\"collectedAt\":\"2026-09-29T10:00:00+08:00\",\"inflow\":100,\"outflow\":40,\"netAmount\":60}]"
        )).stocks().get(0);

        assertThat(stock.effectiveTradeDate()).isEqualTo(TRADE_DATE);
        assertThat(stock.fundSeries()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private StockMonitorDtos.Dashboard dashboardFrom(Map<String, String> cache) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("stock:monitor:v1:enabled".equals(key)) {
                return "[{\"symbol\":\"SH600000\",\"code\":\"600000\",\"name\":\"浦发银行\",\"market\":\"SH\"}]";
            }
            return cache.get(key);
        });
        StockMonitorProfileMapper profiles = mock(StockMonitorProfileMapper.class);
        when(profiles.selectList(any(SFunction.class), anyCollection())).thenReturn(List.of());
        StockMonitorProperty property = new StockMonitorProperty();
        property.setXqEnabled(true);
        return new StockMonitorServiceImpl(null, null, profiles, redis, null, null,
                new ObjectMapper(), property).dashboard();
    }

    private StockMonitorDtos.Quote readQuote() {
        return ReflectionTestUtils.invokeMethod(service, "readQuote", SYMBOL, TRADE_DATE);
    }

    private void assertEmptyError(StockMonitorDtos.Quote quote) {
        assertThat(quote.status()).isEqualTo("ERROR");
        assertThat(quote.sourceTime()).isNull();
        assertThat(quote.price()).isNull();
        assertThat(quote.low()).isNull();
        assertThat(quote.volume()).isNull();
        assertThat(quote.previousClose()).isNull();
    }
}
