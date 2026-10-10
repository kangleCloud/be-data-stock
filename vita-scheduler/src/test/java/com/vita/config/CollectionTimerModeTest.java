package com.vita.config;

import com.vita.marketdata.etfmonitor.service.EtfMonitorRefreshService;
import com.vita.marketdata.stockmonitor.service.StockMonitorRefreshService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class CollectionTimerModeTest {
    @Test
    void timersUseOnlyDefaultAutoEntry() {
        var stocks=mock(StockMonitorRefreshService.class);var etfs=mock(EtfMonitorRefreshService.class);
        new StockMonitorDailyJob(stocks).refresh();new EtfMonitorDailyJob(etfs).refresh();
        verify(stocks).refresh();verify(etfs).refresh();verifyNoMoreInteractions(stocks,etfs);
    }
}
