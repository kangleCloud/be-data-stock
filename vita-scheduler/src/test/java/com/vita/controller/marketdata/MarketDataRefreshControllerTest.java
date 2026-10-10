package com.vita.controller.marketdata;

import com.vita.config.MarketDataLocalAuthConfiguration;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.enums.PythonJobKind;
import com.vita.marketdata.etfmonitor.service.EtfMonitorRefreshService;
import com.vita.marketdata.service.PythonJobsService;
import com.vita.marketdata.stockmonitor.service.StockMonitorRefreshService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketDataRefreshControllerTest {
    @Test
    void inboundModeCannotChangeServerManualChoiceForAnyLocalEntry() throws Exception {
        var stocks=mock(StockMonitorRefreshService.class);var etfs=mock(EtfMonitorRefreshService.class);var jobs=mock(PythonJobsService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new MarketDataRefreshController(stocks,etfs,jobs)).build();
        var paths=new MarketDataLocalAuthConfiguration().marketDataLocalPaths().paths();
        for(String mode:java.util.List.of("manual","auto","invalid")) {
            for(String path:paths) mvc.perform(post(path).header("X-Collection-Mode",mode)
                    .with(request->{request.setRemoteAddr("127.0.0.1");return request;})).andExpect(status().isOk());
        }
        verify(stocks,times(3)).refreshDictionary(CollectionMode.MANUAL);verify(stocks,times(3)).refreshProfiles(CollectionMode.MANUAL);
        verify(etfs,times(3)).refreshDictionary(CollectionMode.MANUAL);verify(etfs,times(3)).refreshProfiles(CollectionMode.MANUAL);
        for(PythonJobKind kind:PythonJobKind.values())verify(jobs,times(3)).refresh(kind,CollectionMode.MANUAL);
        verifyNoMoreInteractions(stocks,etfs,jobs);
    }

    @Test
    void routesEightFixedPostOperationsAndRemovesOldEntries() throws Exception {
        var stocks = mock(StockMonitorRefreshService.class);
        var etfs = mock(EtfMonitorRefreshService.class);
        var jobs = mock(PythonJobsService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MarketDataRefreshController(stocks, etfs, jobs)).build();
        var paths = new MarketDataLocalAuthConfiguration().marketDataLocalPaths().paths();
        assertEquals(8, paths.size());
        for (String path : paths) {
            mvc.perform(post(path).header("X-Collection-Mode", "auto").with(request -> { request.setRemoteAddr("127.0.0.1"); return request; }))
                    .andExpect(status().isOk());
            mvc.perform(get(path)).andExpect(status().isMethodNotAllowed());
        }
        verify(stocks).refreshDictionary(CollectionMode.MANUAL);
        verify(stocks).refreshProfiles(CollectionMode.MANUAL);
        verify(etfs).refreshDictionary(CollectionMode.MANUAL);
        verify(etfs).refreshProfiles(CollectionMode.MANUAL);
        for (PythonJobKind kind : PythonJobKind.values()) verify(jobs).refresh(kind, CollectionMode.MANUAL);
        verifyNoMoreInteractions(stocks, etfs, jobs);
        for (String path : new String[]{"/internal/stock-monitor/v1/refresh",
                "/local/stock-monitor/v1/dictionary/refresh", "/local/stock-monitor/v1/profiles/refresh",
                "/local/python-jobs/v1/calendar/refresh", "/local/python-jobs/v1/market/refresh",
                "/local/python-jobs/v1/monitor/refresh", "/local/python-jobs/v1/etf/refresh"}) {
            mvc.perform(post(path)).andExpect(status().isNotFound());
            assertFalse(paths.contains(path));
        }
    }

    @Test
    void acceptsIpv6LoopbackAndRejectsRemoteOrForwardingBeforeCallingBusiness() throws Exception {
        var stocks = mock(StockMonitorRefreshService.class);
        var etfs = mock(EtfMonitorRefreshService.class);
        var jobs = mock(PythonJobsService.class);
        var controller = new MarketDataRefreshController(stocks, etfs, jobs);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var paths = new MarketDataLocalAuthConfiguration().marketDataLocalPaths().paths();
        for (String path : paths) {
            for (String address : new String[]{"192.168.1.5", "127.0.0.2"}) {
                Exception error = assertThrows(Exception.class, () -> mvc.perform(post(path)
                        .with(request -> { request.setRemoteAddr(address); return request; })));
                assertInstanceOf(ServiceException.class, error.getCause());
            }
            for (String header : new String[]{"Forwarded", "X-Forwarded-For", "X-Real-IP"}) {
                Exception error = assertThrows(Exception.class, () -> mvc.perform(post(path).header(header, "")
                        .with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })));
                assertInstanceOf(ServiceException.class, error.getCause());
            }
        }
        verifyNoInteractions(stocks, etfs, jobs);
        for (String address : new String[]{"::1", "0:0:0:0:0:0:0:1"}) {
            mvc.perform(post(paths.get(0)).with(request -> { request.setRemoteAddr(address); return request; }))
                    .andExpect(status().isOk());
        }
    }
}
