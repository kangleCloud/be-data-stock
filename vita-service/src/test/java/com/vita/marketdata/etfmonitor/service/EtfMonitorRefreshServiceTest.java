package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.etfmonitor.constant.EtfMonitorConstants;
import com.vita.marketdata.etfmonitor.dto.EtfMonitorDtos;
import com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile;
import com.vita.marketdata.etfmonitor.entity.EtfSymbolDictionary;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfSymbolDictionaryMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EtfMonitorRefreshServiceTest {
    @Test
    void overallRefreshWorksWithoutXqAndReturnsPartialWithoutAllocation() throws Exception {
        ObjectMapper json = new ObjectMapper();
        var python = mock(EtfMonitorPythonClient.class);
        var config = mock(EtfMonitorConfigService.class);
        var lock = mock(MarketDataRedisLock.class);
        var transactions = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        when(lock.acquire(anyString(), any())).thenReturn("lease");
        when(config.list()).thenReturn(List.of(new EtfMonitorDtos.AdminEtf(
                "SH510050", "510050", "50ETF", "SH", true, 1), new EtfMonitorDtos.AdminEtf(
                "SZ159915", "159915", "创业板ETF", "SZ", true, 2)));
        when(python.dictionary()).thenReturn(json.readTree("""
                {"schemaVersion":1,"source":"SINA","collectedAt":"2026-10-03T10:00:00+08:00",
                 "etfs":[{"symbol":"SH510050","code":"510050","market":"SH","name":"50ETF"}]}
                """));
        when(python.profiles(anyMap())).thenReturn(json.readTree("""
                {"schemaVersion":1,"source":"THS","collectedAt":"2026-10-03T10:03:00+08:00",
                 "profiles":[{"symbol":"SH510050","source":"THS","fullName":"上证50ETF",
                    "collectedAt":"2026-10-03T10:01:00+08:00"}],
                 "sourceStatus":{"SH510050":"OK","SZ159915":"ERROR"}}
                """));
        var dictionary = mock(EtfSymbolDictionaryMapper.class);
        var profiles = mock(EtfMonitorProfileMapper.class);
        var allocations = mock(EtfAssetAllocationReportMapper.class);
        var service = new EtfMonitorRefreshService(python, dictionary, profiles, allocations, config,
                lock, transactions, json, new StockMonitorProperty());
        var result = service.refresh();
        assertEquals("PARTIAL", result.status());
        assertNotNull(result.startedAt());
        assertNotNull(result.finishedAt());
        assertFalse(json.valueToTree(result).has("jobId"));
        verify(python).dictionary();
        verify(python).profiles(argThat(body -> java.util.Map.of("symbols", List.of("SH510050", "SZ159915")).equals(body)));
        verifyNoMoreInteractions(python);
        verify(dictionary).upsertBatch(anyList());
        verify(config).rebuildEnabledCache();
        verify(config).publishResync();
        verifyNoInteractions(allocations);
        verify(lock).release(EtfMonitorConstants.REFRESH_LOCK, "lease");
    }

    @Test
    void lockConflictDoesNotCallAnySource() {
        var python = mock(EtfMonitorPythonClient.class);
        var lock = mock(MarketDataRedisLock.class);
        var service = new EtfMonitorRefreshService(python, mock(EtfSymbolDictionaryMapper.class),
                mock(EtfMonitorProfileMapper.class), mock(EtfAssetAllocationReportMapper.class),
                mock(EtfMonitorConfigService.class), lock, mock(TransactionTemplate.class),
                new ObjectMapper(), new StockMonitorProperty());
        ServiceException error = assertThrows(ServiceException.class, service::refresh);
        assertEquals(423, error.getCode());
        verifyNoInteractions(python);
    }

    @Test
    void successfulRowsUseIndividualTimesAndExplicitReplacementWithoutInferringIndex() throws Exception {
        var python = mock(EtfMonitorPythonClient.class);
        var config = config("SH510050", "SZ159915");
        var profiles = mock(EtfMonitorProfileMapper.class);
        var dictionary = mock(EtfSymbolDictionaryMapper.class);
        EtfSymbolDictionary dictionaryRow = new EtfSymbolDictionary();
        dictionaryRow.setEtfType("股票ETF");
        when(dictionary.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class), anyString()))
                .thenReturn(dictionaryRow);
        EtfMonitorProfile old = new EtfMonitorProfile();
        old.setId(7L);
        old.setListingStatus("上市");
        old.setListingDate(java.time.LocalDate.of(2020, 1, 1));
        old.setSource(null);
        when(profiles.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class), anyString()))
                .thenReturn(old);
        when(python.profiles(any())).thenReturn(new ObjectMapper().readTree("""
                {"schemaVersion":1,"source":"THS","collectedAt":"2026-10-03T10:03:00+08:00",
                 "sourceStatus":{"SH510050":"OK","SZ159915":"OK"},"profiles":[
                  {"symbol":"SH510050","code":"510050","source":"THS","fullName":"上证50ETF",
                   "fundType":"股票型","fundManager":"经理甲","manager":"管理公司甲",
                   "establishedDate":"2019-12-01","performanceBenchmark":"某指数收益率",
                   "collectedAt":"2026-10-03T10:01:00+08:00"},
                  {"symbol":"SZ159915","source":"THS","fullName":"创业板ETF",
                   "collectedAt":"2026-10-03T10:02:00+08:00"}]}
                """));
        assertEquals("SUCCESS", service(python, config, dictionary, profiles).refreshProfiles().status());
        var rows = ArgumentCaptor.forClass(EtfMonitorProfile.class);
        verify(profiles, times(2)).updateThsProfile(rows.capture());
        verify(profiles, never()).updateById(any(EtfMonitorProfile.class));
        assertEquals(LocalDateTime.of(2026, 10, 3, 10, 1), rows.getAllValues().get(0).getProfileUpdatedAt());
        assertEquals(LocalDateTime.of(2026, 10, 3, 10, 2), rows.getAllValues().get(1).getProfileUpdatedAt());
        var row = rows.getAllValues().get(0);
        assertEquals("THS", row.getSource());
        assertEquals("股票ETF", row.getEtfType());
        assertEquals("股票型", row.getFundType());
        assertEquals("经理甲", row.getFundManager());
        assertEquals("管理公司甲", row.getManager());
        assertNull(row.getTrackingIndexCode());
        assertNull(row.getListingStatus());
        assertNull(row.getListingDate());
        assertNull(row.getShareCount());
        assertNull(row.getShareDate());
        verify(config).publishResync();
    }

    @Test
    void invalidSuccessfulProfileIsPartialAndDoesNotOverwriteThatSymbol() throws Exception {
        var python = mock(EtfMonitorPythonClient.class);
        var config = config("SH510050", "SZ159915");
        var profiles = mock(EtfMonitorProfileMapper.class);
        when(python.profiles(any())).thenReturn(new ObjectMapper().readTree("""
                {"schemaVersion":1,"source":"THS","collectedAt":"2026-10-03T10:03:00+08:00",
                 "sourceStatus":{"SH510050":"OK","SZ159915":"OK"},"profiles":[
                  {"symbol":"SH510050","source":"THS","fullName":"上证50ETF",
                   "collectedAt":"2026-10-03T10:01:00+08:00"},
                  {"symbol":"SZ159915","source":"THS","fullName":"创业板ETF","collectedAt":"invalid"}]}
                """));
        assertEquals("PARTIAL", service(python, config, mock(EtfSymbolDictionaryMapper.class), profiles)
                .refreshProfiles().status());
        var row = ArgumentCaptor.forClass(EtfMonitorProfile.class);
        verify(profiles).insert(row.capture());
        assertEquals("SH510050", row.getValue().getSymbol());
        verify(profiles, never()).selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class), eq("SZ159915"));
    }

    @Test
    void noValidProfileFailsWithoutWritesOrResync() throws Exception {
        var python = mock(EtfMonitorPythonClient.class);
        var config = config("SH510050");
        var profiles = mock(EtfMonitorProfileMapper.class);
        when(python.profiles(any())).thenReturn(new ObjectMapper().readTree("""
                {"schemaVersion":1,"source":"THS","collectedAt":"2026-10-03T10:03:00+08:00",
                 "sourceStatus":{"SH510050":"ERROR"},"profiles":[]}
                """));
        var exception = assertThrows(ServiceException.class,
                () -> service(python, config, mock(EtfSymbolDictionaryMapper.class), profiles).refreshProfiles());
        assertEquals(503, exception.getCode());
        verifyNoInteractions(profiles);
        verify(config, never()).publishResync();
    }

    @Test
    void rejectsCodeSourceAndStatusMismatchBeforePersistence() throws Exception {
        for (String payload : List.of(
                "{\"symbol\":\"SZ159915\",\"source\":\"THS\"}",
                "{\"symbol\":\"SH510050\",\"source\":\"SSE\",\"fullName\":\"旧资料\",\"collectedAt\":\"2026-10-03T10:01:00+08:00\"}")) {
            var python = mock(EtfMonitorPythonClient.class);
            var profiles = mock(EtfMonitorProfileMapper.class);
            when(python.profiles(any())).thenReturn(new ObjectMapper().readTree(
                    "{\"schemaVersion\":1,\"source\":\"THS\",\"collectedAt\":\"2026-10-03T10:03:00+08:00\","
                    + "\"sourceStatus\":{\"SH510050\":\"OK\"},\"profiles\":[" + payload + "]}"));
            assertThrows(ServiceException.class,
                    () -> service(python, config("SH510050"), mock(EtfSymbolDictionaryMapper.class), profiles)
                            .refreshProfiles());
            verifyNoInteractions(profiles);
        }
    }

    private EtfMonitorConfigService config(String... symbols) {
        var config = mock(EtfMonitorConfigService.class);
        when(config.list()).thenReturn(java.util.Arrays.stream(symbols).map(symbol ->
                new EtfMonitorDtos.AdminEtf(symbol, symbol.substring(2), "ETF", symbol.substring(0, 2), true, 1)).toList());
        return config;
    }

    private EtfMonitorRefreshService service(EtfMonitorPythonClient python, EtfMonitorConfigService config,
                                            EtfSymbolDictionaryMapper dictionary, EtfMonitorProfileMapper profiles) {
        var lock = mock(MarketDataRedisLock.class);
        when(lock.acquire(anyString(), any())).thenReturn("lease");
        var transactions = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        return new EtfMonitorRefreshService(python, dictionary, profiles, mock(EtfAssetAllocationReportMapper.class),
                config, lock, transactions, new ObjectMapper(), new StockMonitorProperty());
    }
}
