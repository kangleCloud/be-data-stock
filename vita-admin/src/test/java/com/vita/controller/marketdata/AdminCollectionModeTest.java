package com.vita.controller.marketdata;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.controller.etfmonitor.EtfMonitorAdminController;
import com.vita.controller.stockmonitor.StockMonitorAdminController;
import com.vita.marketdata.etfmonitor.service.EtfMonitorConfigService;
import com.vita.marketdata.etfmonitor.service.EtfMonitorRefreshService;
import com.vita.marketdata.stockmonitor.service.StockMonitorRefreshService;
import com.vita.marketdata.stockmonitor.service.StockMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCollectionModeTest {
    @Test
    void callerModeHeaderCannotUpgradeAdminRefreshAndPermissionsRemain() throws Exception {
        var stocks=mock(StockMonitorRefreshService.class);var etfs=mock(EtfMonitorRefreshService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new StockMonitorAdminController(mock(StockMonitorService.class),stocks),
                new EtfMonitorAdminController(mock(EtfMonitorConfigService.class),etfs)).build();
        for(String path:java.util.List.of("/system/stockMonitor/refresh","/system/etfMonitor/refresh")) {
            mvc.perform(post(path).header("X-Collection-Mode","manual")).andExpect(status().isOk());
        }
        verify(stocks).refresh();verify(etfs).refresh();verifyNoMoreInteractions(stocks,etfs);
        assertArrayEquals(new String[]{"system:stock-monitor:refresh"},StockMonitorAdminController.class.getMethod("refresh")
                .getAnnotation(SaCheckPermission.class).value());
        assertArrayEquals(new String[]{"system:etf-monitor:refresh"},EtfMonitorAdminController.class.getMethod("refresh")
                .getAnnotation(SaCheckPermission.class).value());
    }
}
