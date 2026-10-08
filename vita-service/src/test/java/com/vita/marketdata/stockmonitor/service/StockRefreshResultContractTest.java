package com.vita.marketdata.stockmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.marketdata.stockmonitor.dto.StockMonitorDtos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StockRefreshResultContractTest {
    @Test
    void synchronousResultHasTerminalStateAndNoJobId() throws Exception {
        var result = new StockMonitorDtos.RefreshStatus(true, "SUCCESS",
                "2026-10-03T10:00:00+08:00", "2026-10-03T10:00:05+08:00", null);
        var node = new ObjectMapper().valueToTree(result);
        assertEquals("SUCCESS", node.path("status").asText());
        assertFalse(node.has("jobId"));
    }
}
