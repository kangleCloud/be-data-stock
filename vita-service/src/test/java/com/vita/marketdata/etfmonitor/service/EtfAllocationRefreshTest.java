package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.etfmonitor.dto.EtfMonitorDtos;
import com.vita.marketdata.etfmonitor.entity.EtfAssetAllocationReport;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfSymbolDictionaryMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EtfAllocationRefreshTest {
    private final ObjectMapper json = new ObjectMapper();
    private EtfMonitorPythonClient python;
    private EtfMonitorConfigService config;
    private EtfAssetAllocationReportMapper reports;
    private MarketDataRedisLock lock;
    private TransactionTemplate transactions;

    @BeforeEach
    void enabledEtf() {
        python = mock(EtfMonitorPythonClient.class);
        config = mock(EtfMonitorConfigService.class);
        reports = mock(EtfAssetAllocationReportMapper.class);
        lock = mock(MarketDataRedisLock.class);
        transactions = mock(TransactionTemplate.class);
        when(config.list()).thenReturn(List.of(new EtfMonitorDtos.AdminEtf("SH510050", "510050", "50ETF", "SH", true, 1)));
        when(lock.acquire(anyString(), any())).thenReturn("lease");
        doAnswer(call -> {
            Consumer<TransactionStatus> action = call.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void synchronousSuccessPersistsRequestedReportAndThenResyncs(boolean existing) throws Exception {
        if (existing) {
            var old = new EtfAssetAllocationReport();
            old.setId(9L);
            when(reports.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(old);
        }
        when(python.assetAllocation(any())).thenReturn(json.readTree("""
                {"schemaVersion":1,"symbol":"SH510050","source":"XQ_DANJUAN",
                 "requestedReportPeriod":"2026-06-30","collectedAt":"2026-10-09T10:00:00+08:00",
                 "categories":[{"category":"股票","percent":91.2}]}
                """));
        var result = service(true).refreshAllocation("SH510050", "20260630");
        assertEquals("SUCCESS", result.status());
        assertNotNull(result.finishedAt());
        assertFalse(json.valueToTree(result).has("jobId"));
        verify(python).assetAllocation(Map.of("symbol", "SH510050", "reportPeriod", "20260630"));
        var capture = ArgumentCaptor.forClass(EtfAssetAllocationReport.class);
        var order = inOrder(reports, config);
        if (existing) order.verify(reports).updateById(capture.capture());
        else order.verify(reports).insert(capture.capture());
        order.verify(config).publishResync();
        assertEquals(LocalDate.of(2026, 6, 30), capture.getValue().getRequestedReportPeriod());
        assertEquals("XQ_DANJUAN", capture.getValue().getSource());
        assertEquals(1, json.readTree(capture.getValue().getCategoriesJson()).size());
        if (existing) assertEquals(9L, capture.getValue().getId());
    }

    @Test
    void disabledGateDoesNotCallPythonOrChangeHistory() {
        var error = assertThrows(ServiceException.class, () -> service(false).refreshAllocation("SH510050", "20260630"));
        assertEquals(503, error.getCode());
        assertTrue(error.getMessage().contains("总闸"));
        verifyNoInteractions(python, reports, lock);
    }

    @ParameterizedTest
    @ValueSource(strings = {"20260230", "2026-06-30", "20261301"})
    void invalidReportDateIsRejectedBeforePython(String date) {
        assertEquals(400, assertThrows(ServiceException.class, () -> service(true).refreshAllocation("SH510050", date)).getCode());
        verifyNoInteractions(python, reports);
    }

    @Test
    void unenabledSymbolIsRejectedBeforePython() {
        when(config.list()).thenReturn(List.of());
        assertEquals(400, assertThrows(ServiceException.class, () -> service(true).refreshAllocation("SH510050", "20260630")).getCode());
        verifyNoInteractions(python, reports);
    }

    @ParameterizedTest
    @ValueSource(ints = {423, 429, 503})
    void busyRateLimitOrSourceFailurePreservesOldReports(int code) {
        when(python.assetAllocation(any())).thenThrow(new ServiceException(code, "固定分类诊断"));
        assertEquals(code, assertThrows(ServiceException.class, () -> service(true).refreshAllocation("SH510050", "20260630")).getCode());
        verifyNoInteractions(reports);
        verify(config, never()).publishResync();
    }

    @Test
    void malformedSourceResponseDoesNotOverwriteOldReport() throws Exception {
        when(python.assetAllocation(any())).thenReturn(json.readTree("{\"schemaVersion\":1,\"categories\":[]}"));
        assertEquals(503, assertThrows(ServiceException.class, () -> service(true).refreshAllocation("SH510050", "20260630")).getCode());
        verifyNoInteractions(reports);
        verify(config, never()).publishResync();
    }

    private EtfMonitorRefreshService service(boolean enabled) {
        var property = new StockMonitorProperty();
        property.setXqEnabled(enabled);
        return new EtfMonitorRefreshService(python, mock(EtfSymbolDictionaryMapper.class),
                mock(EtfMonitorProfileMapper.class), reports, config, lock, transactions, json, property);
    }
}
