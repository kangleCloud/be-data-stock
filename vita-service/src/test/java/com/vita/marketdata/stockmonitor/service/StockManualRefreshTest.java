package com.vita.marketdata.stockmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.stockmonitor.constant.StockMonitorConstants;
import com.vita.marketdata.stockmonitor.dto.StockMonitorDtos;
import com.vita.marketdata.stockmonitor.entity.StockMonitorConfig;
import com.vita.marketdata.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.marketdata.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.marketdata.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockManualRefreshTest {
    private final StockMonitorPythonClient python=mock(StockMonitorPythonClient.class);
    private final StockSymbolDictionaryMapper dictionary=mock(StockSymbolDictionaryMapper.class);
    private final StockMonitorConfigMapper configs=mock(StockMonitorConfigMapper.class);
    private final StockMonitorProfileMapper profiles=mock(StockMonitorProfileMapper.class);
    private final StockMonitorService monitor=mock(StockMonitorService.class);
    private final MarketDataRedisLock lock=mock(MarketDataRedisLock.class);
    private final StringRedisTemplate redis=mock(StringRedisTemplate.class);
    private final PlatformTransactionManager tx=mock(PlatformTransactionManager.class);
    private ValueOperations<String,String> values;
    private StockMonitorRefreshService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        when(values.get(StockMonitorConstants.STATUS_KEY)).thenReturn("{\"accepted\":true,\"status\":\"RUNNING\",\"startedAt\":\"2026-10-10T10:00:00+08:00\"}");
        when(lock.acquire(eq(StockMonitorConstants.CONFIG_LOCK),any())).thenReturn("write-lease");
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(python.exchangeDictionary(CollectionMode.MANUAL)).thenReturn(dictionaryItems());
        when(monitor.enabledSymbols()).thenReturn(List.of("SH600000"));
        when(python.profiles(List.of("SH600000"),CollectionMode.MANUAL)).thenReturn(List.of(
                new StockMonitorPythonClient.ProfileItem("SH600000","行业",null,null,"2026-10-10T10:00:00+08:00")));
        var config=new StockMonitorConfig();config.setEnabled(true);
        when(configs.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),anyString())).thenReturn(config);
        service=service(true);
    }

    @Test
    void manualDictionaryBypassesBusyAutoAndIntervalButKeepsWriteTransactionAndAutoStatus() {
        var before=service.status();
        var result=service.refreshDictionary(CollectionMode.MANUAL);
        assertEquals("SUCCESS",result.status());assertTrue(result.accepted());assertNotNull(result.finishedAt());
        assertEquals(before,service.status());
        verify(lock,never()).acquire(eq(StockMonitorConstants.REFRESH_LOCK),any());
        verify(values,never()).setIfAbsent(anyString(),anyString(),any(Duration.class));
        verify(values,never()).set(anyString(),anyString());
        var order=inOrder(lock,tx,dictionary,monitor);
        order.verify(lock).acquire(eq(StockMonitorConstants.CONFIG_LOCK),any());
        order.verify(tx).getTransaction(any());order.verify(dictionary).upsertBatch(anyList());
        order.verify(tx).commit(any());order.verify(monitor).rebuildEnabledCache();
        order.verify(lock).release(StockMonitorConstants.CONFIG_LOCK,"write-lease");
        assertFalse(new ObjectMapper().valueToTree(result).has("jobId"));
    }

    @Test
    void manualProfilesKeepsConfigLockEnabledCheckTransactionAndResyncWithoutStatusWrites() {
        assertEquals("SUCCESS",service.refreshProfiles(CollectionMode.MANUAL).status());
        verify(python).profiles(List.of("SH600000"),CollectionMode.MANUAL);
        verify(profiles).insert(any(com.vita.marketdata.stockmonitor.entity.StockMonitorProfile.class));
        verify(tx).commit(any());verify(monitor).publishResync();
        verify(lock).release(StockMonitorConstants.CONFIG_LOCK,"write-lease");
        verify(lock,never()).acquire(eq(StockMonitorConstants.REFRESH_LOCK),any());verifyNoInteractions(redis,values);
    }

    @Test
    void manualCannotBypassConfigWriteLock() {
        when(lock.acquire(eq(StockMonitorConstants.CONFIG_LOCK),any())).thenReturn(null);
        assertEquals("ERROR",service.refreshDictionary(CollectionMode.MANUAL).status());
        verifyNoInteractions(dictionary,tx,redis,values);verify(monitor,never()).rebuildEnabledCache();
    }

    @Test
    void manualCannotBypassXqGateOrEnableLimit() {
        assertEquals(503,assertThrows(ServiceException.class,()->service(false).refreshProfiles(CollectionMode.MANUAL)).getCode());
        verify(python,never()).profiles(anyList(),any());
        when(monitor.enabledSymbols()).thenReturn(java.util.Collections.nCopies(11,"SH600000"));
        assertEquals("ERROR",service.refreshProfiles(CollectionMode.MANUAL).status());
        verify(python,never()).profiles(anyList(),any());verifyNoInteractions(profiles,tx,redis,values);
    }

    @Test
    void invalidSourceRollsBackBeforeAnyWriteWithoutChangingStatus() {
        when(python.exchangeDictionary(CollectionMode.MANUAL)).thenReturn(List.of(
                new StockMonitorDtos.DictionaryItem("SH600000","BAD","中文","SH")));
        assertEquals("ERROR",service.refreshDictionary(CollectionMode.MANUAL).status());
        verify(tx).rollback(any());verify(tx,never()).commit(any());verifyNoInteractions(dictionary,redis,values);
        verify(monitor,never()).rebuildEnabledCache();
    }

    @Test
    void persistenceFailureRollsBackAndDoesNotPublish() {
        doThrow(new IllegalStateException("test write failure")).when(profiles).insert(any(com.vita.marketdata.stockmonitor.entity.StockMonitorProfile.class));
        assertEquals("ERROR",service.refreshProfiles(CollectionMode.MANUAL).status());
        verify(tx).rollback(any());verify(monitor,never()).publishResync();
        verify(lock).release(StockMonitorConstants.CONFIG_LOCK,"write-lease");verifyNoInteractions(redis,values);
    }

    @Test
    void defaultAutoStillRespectsRefreshLock() {
        var result=service.refreshDictionary();assertFalse(result.accepted());assertEquals("RUNNING",result.status());
        verify(lock).acquire(eq(StockMonitorConstants.REFRESH_LOCK),any());verifyNoInteractions(python,dictionary,tx);
    }

    @Test
    void defaultAutoStillRespectsTriggerIntervalAndReleasesLock() {
        when(lock.acquire(eq(StockMonitorConstants.REFRESH_LOCK),any())).thenReturn("auto-lease");
        when(values.setIfAbsent(eq(StockMonitorConstants.DICTIONARY_INTERVAL_KEY),anyString(),any(Duration.class))).thenReturn(false);
        assertEquals(429,assertThrows(ServiceException.class,service::refreshDictionary).getCode());
        verify(lock).release(StockMonitorConstants.REFRESH_LOCK,"auto-lease");verifyNoInteractions(python);
    }

    @Test
    void defaultAutoWritesRunningAndTerminalStatusWithAutoSourceMode() {
        when(lock.acquire(eq(StockMonitorConstants.REFRESH_LOCK),any())).thenReturn("auto-lease");
        when(values.setIfAbsent(anyString(),anyString(),any(Duration.class))).thenReturn(true);
        when(python.exchangeDictionary(CollectionMode.AUTO)).thenReturn(dictionaryItems());
        assertEquals("SUCCESS",service.refreshDictionary().status());
        verify(values).set(eq(StockMonitorConstants.STATUS_KEY),contains("RUNNING"));
        verify(values).set(eq(StockMonitorConstants.STATUS_KEY),contains("SUCCESS"));
        verify(python).exchangeDictionary(CollectionMode.AUTO);verify(tx).commit(any());
    }

    @Test
    void adminAndTimerOverallEntryKeepsDictionaryAndProfilesAuto() {
        when(lock.acquire(eq(StockMonitorConstants.REFRESH_LOCK),any())).thenReturn("auto-lease");
        when(python.exchangeDictionary(CollectionMode.AUTO)).thenReturn(dictionaryItems());
        when(python.profiles(List.of("SH600000"),CollectionMode.AUTO)).thenReturn(List.of(
                new StockMonitorPythonClient.ProfileItem("SH600000","行业",null,null,"2026-10-10T10:00:00+08:00")));
        assertEquals("SUCCESS",service.refresh().status());
        verify(python).exchangeDictionary(CollectionMode.AUTO);verify(python).profiles(List.of("SH600000"),CollectionMode.AUTO);
        verify(values).set(eq(StockMonitorConstants.STATUS_KEY),contains("RUNNING"));
        verify(values).set(eq(StockMonitorConstants.STATUS_KEY),contains("SUCCESS"));
        verify(tx,times(2)).commit(any());verify(monitor).publishResync();
    }

    private StockMonitorRefreshService service(boolean enabled) {
        var property=new StockMonitorProperty();property.setXqEnabled(enabled);
        return new StockMonitorRefreshService(python,dictionary,configs,profiles,monitor,lock,redis,new ObjectMapper(),property,new TransactionTemplate(tx));
    }

    private List<StockMonitorDtos.DictionaryItem> dictionaryItems() {
        return List.of(new StockMonitorDtos.DictionaryItem("SH600000","600000","中文","SH"));
    }
}
