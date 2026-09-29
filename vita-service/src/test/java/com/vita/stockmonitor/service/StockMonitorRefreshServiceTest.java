package com.vita.stockmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.ServiceException;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.entity.StockMonitorConfig;
import com.vita.stockmonitor.entity.StockMonitorProfile;
import com.vita.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.stockmonitor.property.StockMonitorProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StockMonitorRefreshServiceTest {
    private final StockMonitorPythonClient pythonClient = mock(StockMonitorPythonClient.class);
    private final StockSymbolDictionaryMapper dictionaryMapper = mock(StockSymbolDictionaryMapper.class);
    private final StockMonitorConfigMapper configMapper = mock(StockMonitorConfigMapper.class);
    private final StockMonitorProfileMapper profileMapper = mock(StockMonitorProfileMapper.class);
    private final StockMonitorService monitorService = mock(StockMonitorService.class);
    private final StockMonitorRedisLock redisLock = mock(StockMonitorRedisLock.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);

    @BeforeEach
    void allowConfigLock() {
        when(redisLock.acquire(eq("stock:monitor:v1:config:lock"), any(Duration.class)))
                .thenReturn("config-token");
    }

    @Test
    void disabledSwitchRefreshesExchangeDictionaryWithoutCallingXueqiuProfiles() {
        when(redisLock.acquire(eq("stock:monitor:v1:refresh:lock"), any(Duration.class)))
                .thenReturn("lock-token");
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(pythonClient.exchangeDictionary()).thenReturn(List.of(
                new StockMonitorDtos.DictionaryItem("SH600000", "600000", "浦发银行", "SH")));

        StockMonitorDtos.RefreshStatus result = service(false).refresh();

        assertThat(result.accepted()).isTrue();
        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(pythonClient).exchangeDictionary();
        verify(pythonClient, never()).profiles(any());
        verify(monitorService).rebuildEnabledCache();
        verify(redisLock).release("stock:monitor:v1:config:lock", "config-token");
        verify(redisLock).release("stock:monitor:v1:refresh:lock", "lock-token");
    }

    @Test
    void concurrentRefreshDoesNotCallPython() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        StockMonitorDtos.RefreshStatus result = service(false).refresh();

        assertThat(result.accepted()).isFalse();
        verifyNoInteractions(pythonClient, dictionaryMapper, profileMapper);
    }

    @Test
    void manualDictionaryRefreshNeverCallsProfilesAndUsesTenMinuteInterval() {
        when(redisLock.acquire(eq("stock:monitor:v1:refresh:lock"), any(Duration.class)))
                .thenReturn("lock-token");
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("stock:monitor:v1:refresh:dictionary:interval", "1", Duration.ofMinutes(10)))
                .thenReturn(true);
        when(pythonClient.exchangeDictionary()).thenReturn(List.of(
                new StockMonitorDtos.DictionaryItem("SH600000", "600000", "交易所名称", "SH")));

        StockMonitorDtos.RefreshStatus result = service(true).refreshDictionary();

        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(dictionaryMapper).upsertBatch(argThat(stocks -> stocks.size() == 1
                && "交易所名称".equals(stocks.get(0).getName())));
        verify(monitorService).rebuildEnabledCache();
        verify(redisLock).release("stock:monitor:v1:config:lock", "config-token");
        verify(pythonClient, never()).profiles(any());
    }

    @Test
    void disabledProfilesRefreshStopsBeforeExternalCalls() {
        assertThatThrownBy(() -> service(false).refreshProfiles())
                .isInstanceOfSatisfying(ServiceException.class,
                        error -> assertThat(error.getCode()).isEqualTo(503));
        verifyNoInteractions(redisLock, pythonClient, configMapper, profileMapper);
    }

    @Test
    void profilesRefreshWithNoEnabledStocksAvoidsPythonAndDictionary() {
        when(redisLock.acquire(eq("stock:monitor:v1:refresh:lock"), any(Duration.class)))
                .thenReturn("lock-token");
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("stock:monitor:v1:refresh:profiles:interval", "1", Duration.ofMinutes(30)))
                .thenReturn(true);
        when(monitorService.enabledSymbols()).thenReturn(List.of());

        assertThat(service(true).refreshProfiles().status()).isEqualTo("SUCCESS");
        verifyNoInteractions(pythonClient, dictionaryMapper, profileMapper);
        verify(redisLock).release("stock:monitor:v1:refresh:lock", "lock-token");
    }

    @Test
    void profilesRefreshOnlyFetchesEnabledStocks() {
        when(redisLock.acquire(eq("stock:monitor:v1:refresh:lock"), any(Duration.class)))
                .thenReturn("lock-token");
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("stock:monitor:v1:refresh:profiles:interval", "1", Duration.ofMinutes(30)))
                .thenReturn(true);
        when(monitorService.enabledSymbols()).thenReturn(List.of("SH600000"));
        when(pythonClient.profiles(List.of("SH600000"))).thenReturn(List.of(
                new StockMonitorPythonClient.ProfileItem("SH600000", "银行", "1999-11-10", null,
                        "2026-09-29T15:30:00+08:00")));
        StockMonitorConfig enabled = new StockMonitorConfig();
        enabled.setEnabled(true);
        when(configMapper.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                eq("SH600000"))).thenReturn(enabled);

        assertThat(service(true).refreshProfiles().status()).isEqualTo("SUCCESS");

        verify(pythonClient).profiles(List.of("SH600000"));
        verify(pythonClient, never()).exchangeDictionary();
        verify(dictionaryMapper, never()).upsertBatch(any());
        verify(profileMapper).insert(argThat((StockMonitorProfile profile) -> "SH600000".equals(profile.getSymbol())
                && "银行".equals(profile.getIndustry())));
        verify(redisLock).release("stock:monitor:v1:config:lock", "config-token");
    }

    @Test
    void manualRepeatIsRateLimitedBeforeCallingSource() {
        when(redisLock.acquire(eq("stock:monitor:v1:refresh:lock"), any(Duration.class)))
                .thenReturn("lock-token");
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("stock:monitor:v1:refresh:dictionary:interval", "1", Duration.ofMinutes(10)))
                .thenReturn(false);

        assertThatThrownBy(() -> service(false).refreshDictionary())
                .isInstanceOfSatisfying(ServiceException.class,
                        error -> assertThat(error.getCode()).isEqualTo(429));
        verifyNoInteractions(pythonClient);
        verify(redisLock).release("stock:monitor:v1:refresh:lock", "lock-token");
    }

    private StockMonitorRefreshService service(boolean enabled) {
        StockMonitorProperty property = new StockMonitorProperty();
        property.setXqEnabled(enabled);
        return new StockMonitorRefreshService(pythonClient, dictionaryMapper, configMapper, profileMapper,
                monitorService, redisLock, redisTemplate, new ObjectMapper(), property);
    }
}
