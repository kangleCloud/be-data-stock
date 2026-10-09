package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EtfMonitorStreamPatchTest {
    @Test
    void quotePatchOmitsUnchangedAssetAllocation() throws Exception {
        ObjectMapper json = new ObjectMapper();
        var etf = json.readTree("{\"symbol\":\"SH510050\",\"quote\":{\"price\":2.5},"
                + "\"assetAllocationStatus\":\"AVAILABLE\",\"assetAllocation\":{\"categories\":[{\"category\":\"股票\",\"percent\":91.2}]}}");
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        EtfMonitorStreamService service = new EtfMonitorStreamService(
                mock(EtfMonitorDashboardService.class), json, scheduler);
        try {
            var patch = service.quotePatch(etf);
            assertEquals("SH510050", patch.path("symbol").asText());
            assertEquals(2.5, patch.path("quote").path("price").asDouble());
            assertFalse(patch.has("assetAllocation"));
            assertEquals("AVAILABLE", patch.path("assetAllocationStatus").asText());
            assertTrue(etf.has("assetAllocation"));
        } finally {
            service.shutdown();
        }
    }
}
