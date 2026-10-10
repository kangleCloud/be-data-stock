package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.etfmonitor.constant.EtfMonitorConstants;
import com.vita.marketdata.etfmonitor.dto.EtfMonitorDtos;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfSymbolDictionaryMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EtfManualRefreshTest {
    private final EtfMonitorPythonClient python=mock(EtfMonitorPythonClient.class);
    private final EtfSymbolDictionaryMapper dictionary=mock(EtfSymbolDictionaryMapper.class);
    private final EtfMonitorProfileMapper profiles=mock(EtfMonitorProfileMapper.class);
    private final EtfAssetAllocationReportMapper allocations=mock(EtfAssetAllocationReportMapper.class);
    private final EtfMonitorConfigService config=mock(EtfMonitorConfigService.class);
    private final MarketDataRedisLock lock=mock(MarketDataRedisLock.class);
    private final PlatformTransactionManager tx=mock(PlatformTransactionManager.class);
    private EtfMonitorRefreshService service;

    @BeforeEach
    void setup() throws Exception {
        var json=new ObjectMapper();when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(lock.acquire(eq(EtfMonitorConstants.CONFIG_LOCK),any())).thenReturn("write-lease");
        when(python.dictionary(CollectionMode.MANUAL)).thenReturn(json.readTree("""
                {"schemaVersion":1,"source":"SINA","collectedAt":"2026-10-10T10:00:00+08:00",
                 "etfs":[{"symbol":"SH510050","code":"510050","market":"SH","name":"50ETF"}]}
                """));
        when(config.list()).thenReturn(List.of(new EtfMonitorDtos.AdminEtf("SH510050","510050","50ETF","SH",true,1)));
        when(python.profiles(any(),eq(CollectionMode.MANUAL))).thenReturn(json.readTree("""
                {"schemaVersion":1,"source":"THS","collectedAt":"2026-10-10T10:00:00+08:00",
                 "sourceStatus":{"SH510050":"OK"},"profiles":[{"symbol":"SH510050","source":"THS",
                 "fullName":"50ETF","collectedAt":"2026-10-10T10:00:00+08:00"}]}
                """));
        service=new EtfMonitorRefreshService(python,dictionary,profiles,allocations,config,lock,
                new TransactionTemplate(tx),json,new StockMonitorProperty());
    }

    @Test
    void manualDictionaryWorksDuringBusyAutoWithConfigLockAndTransaction() {
        assertEquals("SUCCESS",service.refreshDictionary(CollectionMode.MANUAL).status());
        verify(lock,never()).acquire(eq(EtfMonitorConstants.REFRESH_LOCK),any());
        verify(dictionary).upsertBatch(anyList());verify(tx).commit(any());verify(config).rebuildEnabledCache();
        verify(lock).release(EtfMonitorConstants.CONFIG_LOCK,"write-lease");verifyNoInteractions(allocations);
        assertEquals(423,assertThrows(ServiceException.class,service::refreshDictionary).getCode());
        verify(python,times(1)).dictionary(CollectionMode.MANUAL);verify(python,never()).dictionary(CollectionMode.AUTO);
    }

    @Test
    void manualProfilesRetainsWriteLockTransactionResyncAndDoesNotRequireXq() {
        assertEquals("SUCCESS",service.refreshProfiles(CollectionMode.MANUAL).status());
        verify(python).profiles(Map.of("symbols",List.of("SH510050")),CollectionMode.MANUAL);
        verify(profiles).insert(any(com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile.class));
        verify(tx).commit(any());verify(config).publishResync();
        verify(lock).release(EtfMonitorConstants.CONFIG_LOCK,"write-lease");verifyNoInteractions(allocations);
        assertEquals(503,assertThrows(ServiceException.class,()->service.refreshAllocation("SH510050","20260630")).getCode());
    }

    @Test
    void manualCannotBypassConfigLockOrLimit() {
        when(lock.acquire(eq(EtfMonitorConstants.CONFIG_LOCK),any())).thenReturn(null);
        assertEquals(503,assertThrows(ServiceException.class,()->service.refreshDictionary(CollectionMode.MANUAL)).getCode());
        verifyNoInteractions(dictionary,tx);verify(config,never()).rebuildEnabledCache();
        when(config.list()).thenReturn(java.util.Collections.nCopies(11,new EtfMonitorDtos.AdminEtf("SH510050","510050","50ETF","SH",true,1)));
        assertEquals(503,assertThrows(ServiceException.class,()->service.refreshProfiles(CollectionMode.MANUAL)).getCode());
        verify(python,never()).profiles(any(),any());verifyNoInteractions(profiles);
    }

    @Test
    void failedManualWriteRollsBackAndDoesNotPublish() {
        doThrow(new IllegalStateException("test write failure")).when(dictionary).upsertBatch(anyList());
        assertThrows(IllegalStateException.class,()->service.refreshDictionary(CollectionMode.MANUAL));
        verify(tx).rollback(any());verify(config,never()).rebuildEnabledCache();
        verify(lock).release(EtfMonitorConstants.CONFIG_LOCK,"write-lease");
    }
}
