package com.vita.marketdata.stockmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.market.constant.MarketConstants;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.stockmonitor.constant.StockMonitorConstants;
import com.vita.marketdata.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.marketdata.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.marketdata.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.marketdata.stockmonitor.service.impl.StockMonitorServiceImpl;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.vita.marketdata.support.SseEventCapture.id;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class StockDashboardVersionTest {
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, String> cache = new HashMap<>();
    private final AtomicInteger version = new AtomicInteger(1);
    private final String date = LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1).toString();
    private final String quoteKey = StockMonitorConstants.QUOTE_PREFIX + "SH600000";
    private final String fundKey = StockMonitorConstants.FUND_SERIES_PREFIX + date + ":SH600000";
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private StockMonitorProfileMapper profiles;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void cacheFixture() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        profiles = mock(StockMonitorProfileMapper.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> {
            String key = call.getArgument(0);
            return StockMonitorConstants.STATE_ID_KEY.equals(key) ? id(version.get()) : cache.get(key);
        });
        cache.put(StockMonitorConstants.ENABLED_KEY,
                "[{\"symbol\":\"SH600000\",\"code\":\"600000\",\"name\":\"测试股票\",\"market\":\"SH\"}]");
        cache.put(StockMonitorConstants.LAST_TRADE_DATE_KEY, date);
        cache.put(MarketConstants.SNAPSHOT_KEY, "{\"modules\":{\"marketFundFlow\":{\"tradeDate\":\""
                + date + "\",\"data\":{}}}}");
        cache.put(StockMonitorConstants.SERIES_PREFIX + date + ":SH600000",
                "[{\"time\":\"" + date + "T15:01:00+08:00\",\"price\":10}]");
        cache.put(quoteKey, quote(10));
        cache.put(fundKey, funds(100));
    }

    @Test
    void stableReadPreservesHistoricalSourceTimesAndSingleFundPoint() {
        var dashboard = service(true).dashboard();
        assertEquals(id(1), dashboard.stateId());
        var stock = dashboard.stocks().get(0);
        assertEquals("HISTORICAL", stock.dataStatus());
        assertEquals(date, stock.effectiveTradeDate());
        assertEquals(date + "T15:01:00+08:00", stock.quote().sourceTime());
        assertEquals("STALE", stock.quote().status());
        assertTrue(stock.closeConfirmed());
        assertEquals(1, stock.fundSeries().size());
        assertEquals(date + "T15:10:00+08:00", stock.fundSeries().get(0).collectedAt());
        verify(values, never()).set(anyString(), anyString());
    }

    @Test
    void quoteAndFundCommitDuringReadRetriesWholeDashboardAgainstNewVersion() {
        var reads = new AtomicInteger();
        when(values.get(quoteKey)).thenAnswer(call -> {
            String old = cache.get(quoteKey);
            if (reads.getAndIncrement() == 0) {
                // 模拟资金与行情在首次聚合读取中提交；旧行情和新资金不能以旧版本返回。
                version.set(2);
                cache.put(quoteKey, quote(20));
                cache.put(fundKey, funds(200));
            }
            return old;
        });
        var dashboard = service(true).dashboard();
        assertEquals(id(2), dashboard.stateId());
        assertEquals(2, reads.get());
        var stock = dashboard.stocks().get(0);
        assertEquals(0, new BigDecimal("20").compareTo(stock.quote().price()));
        assertEquals(1, stock.fundSeries().size());
        assertEquals(0, new BigDecimal("200").compareTo(stock.fundSeries().get(0).inflow()));
        assertEquals("HISTORICAL", stock.dataStatus());
        verify(values, never()).set(anyString(), anyString());
    }

    @Test
    void continuousCommitsRejectMixedDashboardAfterBoundedRetry() {
        when(values.get(quoteKey)).thenAnswer(call -> {
            version.incrementAndGet();
            return cache.get(quoteKey);
        });
        var exception = assertThrows(ServiceException.class, () -> service(true).dashboard());
        assertEquals(503, exception.getCode());
        verify(values, times(2)).get(quoteKey);
    }

    @Test
    void xqDisabledSkipsQuoteFundAndProfileReads() {
        var dashboard = service(false).dashboard();
        var stock = dashboard.stocks().get(0);
        assertFalse(dashboard.xqEnabled());
        assertEquals("DISABLED", stock.quote().status());
        assertEquals("DISABLED", stock.dataStatus());
        assertNull(stock.effectiveTradeDate());
        assertFalse(stock.closeConfirmed());
        assertTrue(stock.fundSeries().isEmpty());
        verify(values, never()).get(quoteKey);
        verify(values, never()).get(fundKey);
        verifyNoInteractions(profiles);
    }

    private StockMonitorServiceImpl service(boolean xqEnabled) {
        var property = new StockMonitorProperty();
        property.setXqEnabled(xqEnabled);
        return new StockMonitorServiceImpl(mock(StockSymbolDictionaryMapper.class),
                mock(StockMonitorConfigMapper.class), profiles, redis, mock(MarketDataRedisLock.class),
                mock(TransactionTemplate.class), json, property);
    }

    private String quote(int price) {
        return "{\"schemaVersion\":1,\"symbol\":\"SH600000\",\"source\":\"XQ\",\"status\":\"STALE\","
                + "\"sourceTime\":\"" + date + "T15:01:00+08:00\",\"collectedAt\":\"" + date
                + "T15:02:00+08:00\",\"tradeDate\":\"" + date + "\",\"price\":" + price + "}";
    }

    private String funds(int inflow) {
        return "[{\"collectedAt\":\"" + date + "T15:10:00+08:00\",\"inflow\":" + inflow
                + ",\"outflow\":40,\"netAmount\":" + (inflow - 40) + "}]";
    }
}
