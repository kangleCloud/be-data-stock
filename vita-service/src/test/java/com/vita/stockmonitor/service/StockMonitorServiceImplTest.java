package com.vita.stockmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.ServiceException;
import com.vita.stockmonitor.dto.StockDictionaryCreateDto;
import com.vita.stockmonitor.entity.StockMonitorConfig;
import com.vita.stockmonitor.entity.StockSymbolDictionary;
import com.vita.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.impl.StockMonitorServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.argThat;

class StockMonitorServiceImplTest {
    private final StockSymbolDictionaryMapper dictionaryMapper = mock(StockSymbolDictionaryMapper.class);
    private final StockMonitorConfigMapper configMapper = mock(StockMonitorConfigMapper.class);
    private final StockMonitorProfileMapper profileMapper = mock(StockMonitorProfileMapper.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final StockMonitorRedisLock redisLock = mock(StockMonitorRedisLock.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);

    @Test
    void manualDictionaryCreateDerivesSymbolWithoutEnablingMonitor() {
        var created = service(false).addDictionaryStock(
                new StockDictionaryCreateDto("SH", "600000", " 浦发银行 "));

        assertThat(created.symbol()).isEqualTo("SH600000");
        assertThat(created.name()).isEqualTo("浦发银行");
        verify(dictionaryMapper).insert(argThat((StockSymbolDictionary stock) -> "SH600000".equals(stock.getSymbol())
                && "浦发银行".equals(stock.getName())));
        verifyNoInteractions(configMapper, profileMapper, redisTemplate);
    }

    @Test
    void manualDictionaryCreateRejectsDuplicateAndInvalidCode() {
        when(dictionaryMapper.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                eq("SH600000"))).thenReturn(new StockSymbolDictionary());

        assertThatThrownBy(() -> service(false).addDictionaryStock(
                new StockDictionaryCreateDto("SH", "600000", "浦发银行")))
                .isInstanceOfSatisfying(ServiceException.class,
                        error -> assertThat(error.getCode()).isEqualTo(400));
        assertThatThrownBy(() -> service(false).addDictionaryStock(
                new StockDictionaryCreateDto("SH", "60000X", "浦发银行")))
                .isInstanceOfSatisfying(ServiceException.class,
                        error -> assertThat(error.getCode()).isEqualTo(400));
        verify(dictionaryMapper, never()).insert(any(StockSymbolDictionary.class));
    }

    @Test
    void startupWarmsOnlyEnabledListWithoutExternalRequests() {
        when(configMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(values);
        StockMonitorServiceImpl service = service(false);

        service.prewarmEnabledCache();

        verify(values).set("stock:monitor:v1:enabled", "[]");
        verifyNoInteractions(dictionaryMapper, profileMapper);
    }

    @Test
    void moreThanTenEnabledStocksCannotBePublished() {
        List<StockMonitorConfig> configs = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            StockMonitorConfig config = new StockMonitorConfig();
            config.setSymbol("SH" + String.format("%06d", i));
            config.setEnabled(true);
            config.setSortOrder(i + 1);
            configs.add(config);
        }
        when(configMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(configs);

        assertThatThrownBy(() -> service(false).rebuildEnabledCache()).isInstanceOf(ServiceException.class);
        verifyNoInteractions(dictionaryMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void enablingEleventhStockIsRejectedBeforeInsert() {
        when(redisLock.acquire(eq("stock:monitor:v1:config:lock"), any(Duration.class)))
                .thenReturn("lock-token");
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        StockSymbolDictionary dictionary = new StockSymbolDictionary();
        dictionary.setSymbol("SH600999");
        when(dictionaryMapper.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                eq("SH600999"))).thenReturn(dictionary);
        List<StockMonitorConfig> active = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            StockMonitorConfig config = new StockMonitorConfig();
            config.setSymbol("SH" + String.format("%06d", i));
            config.setEnabled(true);
            config.setSortOrder(i + 1);
            active.add(config);
        }
        when(configMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(active);

        assertThatThrownBy(() -> service(false).setEnabled("SH600999", true))
                .isInstanceOfSatisfying(ServiceException.class,
                        error -> assertThat(error.getCode()).isEqualTo(400));
        verify(configMapper, never()).insert(any(StockMonitorConfig.class));
        verify(redisLock).release("stock:monitor:v1:config:lock", "lock-token");
    }

    @Test
    void disablingKeepsConfigButRemovesProfileAndRebuildsEnabledCache() {
        when(redisLock.acquire(eq("stock:monitor:v1:config:lock"), any(Duration.class)))
                .thenReturn("lock-token");
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        StockSymbolDictionary dictionary = new StockSymbolDictionary();
        dictionary.setSymbol("SH600000");
        when(dictionaryMapper.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                eq("SH600000"))).thenReturn(dictionary);
        StockMonitorConfig config = new StockMonitorConfig();
        config.setSymbol("SH600000");
        config.setEnabled(true);
        when(configMapper.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                eq("SH600000"))).thenReturn(config);
        when(configMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(values);

        service(false).setEnabled("SH600000", false);

        assertThat(config.getEnabled()).isFalse();
        verify(configMapper).updateById(config);
        verify(profileMapper).deleteBySymbol("SH600000");
        verify(values).set("stock:monitor:v1:enabled", "[]");
    }

    @Test
    void disabledSwitchSuppressesStoredXueqiuQuoteAndSeries() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("stock:monitor:v1:enabled")).thenReturn(
                "[{\"symbol\":\"SH600000\",\"code\":\"600000\",\"name\":\"浦发银行\",\"market\":\"SH\"}]");
        var dashboard = service(false).dashboard();

        assertThat(dashboard.xqEnabled()).isFalse();
        assertThat(dashboard.tradeDate()).isNull();
        assertThat(dashboard.stocks()).hasSize(1);
        assertThat(dashboard.stocks().get(0).quote().status()).isEqualTo("DISABLED");
        assertThat(dashboard.stocks().get(0).quote().price()).isNull();
        assertThat(dashboard.stocks().get(0).series()).isEmpty();
        assertThat(dashboard.stocks().get(0).profile().industry()).isNull();
        assertThat(dashboard.stocks().get(0).profile().listingDate()).isNull();
        assertThat(dashboard.stocks().get(0).profile().marketCap()).isNull();
        assertThat(dashboard.stocks().get(0).profile().updatedAt()).isNull();
        verifyNoInteractions(profileMapper);
        verify(values, never()).get(startsWith("stock:monitor:v1:quote:"));
        verify(values, never()).get(startsWith("stock:monitor:v1:series:"));
    }

    @Test
    void missingEnabledCacheIsUnavailableRatherThanAnEmptyMonitorList() {
        when(redisTemplate.opsForValue()).thenReturn(values);

        assertThatThrownBy(() -> service(false).dashboard())
                .isInstanceOfSatisfying(ServiceException.class,
                        error -> assertThat(error.getCode()).isEqualTo(503));
    }

    @Test
    void duplicateSymbolInEnabledCacheIsUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("stock:monitor:v1:enabled")).thenReturn("""
                [{"symbol":"SH600000","code":"600000","name":"浦发银行","market":"SH"},
                 {"symbol":"SH600000","code":"600000","name":"浦发银行","market":"SH"}]
                """);

        assertThatThrownBy(() -> service(false).dashboard())
                .isInstanceOfSatisfying(ServiceException.class,
                        error -> assertThat(error.getCode()).isEqualTo(503));
        verifyNoInteractions(profileMapper);
    }

    @Test
    void missingQuoteSuppressesOldSeriesFromDashboard() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("stock:monitor:v1:enabled")).thenReturn(
                "[{\"symbol\":\"SH600000\",\"code\":\"600000\",\"name\":\"浦发银行\",\"market\":\"SH\"}]");
        when(values.get("stock:monitor:v1:lastTradeDate")).thenReturn("2026-09-28");
        when(values.get("stock:monitor:v1:series:2026-09-28:SH600000")).thenReturn(
                "[{\"time\":\"2026-09-28T09:31:00+08:00\",\"price\":10.2}]");
        when(profileMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        var dashboard = service(true).dashboard();

        assertThat(dashboard.stocks()).hasSize(1);
        assertThat(dashboard.stocks().get(0).quote().status()).isEqualTo("ERROR");
        assertThat(dashboard.stocks().get(0).series()).isEmpty();
        verify(values, never()).get("stock:monitor:v1:series:2026-09-28:SH600000");
    }

    private StockMonitorServiceImpl service(boolean enabled) {
        StockMonitorProperty property = new StockMonitorProperty();
        property.setXqEnabled(enabled);
        return new StockMonitorServiceImpl(dictionaryMapper, configMapper, profileMapper, redisTemplate,
                redisLock, transactions, new ObjectMapper(), property);
    }
}
