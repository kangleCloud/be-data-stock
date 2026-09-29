package com.vita.config;

import com.vita.controller.stockmonitor.StockMonitorRefreshController;
import com.vita.core.exception.ServiceException;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.StockMonitorRefreshService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StockMonitorInternalAuthTest {
    @Test
    void internalRefreshRequiresConfiguredMatchingToken() {
        StockMonitorRefreshService service = mock(StockMonitorRefreshService.class);
        StockMonitorRefreshController controller = new StockMonitorRefreshController(service, property("secret"));

        assertThatThrownBy(() -> controller.refresh(null)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> controller.refresh("wrong")).isInstanceOf(ServiceException.class);
        verifyNoInteractions(service);
        assertThat(controller.refresh("secret")).isNotNull();
        verify(service).refresh();
    }

    @Test
    void blankConfiguredTokenNeverOpensEndpoint() {
        StockMonitorRefreshService service = mock(StockMonitorRefreshService.class);
        StockMonitorRefreshController controller = new StockMonitorRefreshController(service, property(""));

        assertThatThrownBy(() -> controller.refresh("")).isInstanceOf(ServiceException.class);
        verifyNoInteractions(service);
    }

    private StockMonitorProperty property(String token) {
        StockMonitorProperty property = new StockMonitorProperty();
        property.setInternalToken(token);
        return property;
    }
}
