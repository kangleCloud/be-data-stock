package com.vita.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.controller.market.MarketDashboardController;
import com.vita.market.service.MarketSnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketSnapshotControllerContractTest {

    @Test
    void endpointUsesExpectedPathAndPermission() throws Exception {
        assertThat(MarketDashboardController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/market/dashboard");
        var method = MarketDashboardController.class.getMethod("getSnapshot");
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/snapshot");
        assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("market:dashboard:view");

        MarketSnapshotService service = mock(MarketSnapshotService.class);
        when(service.getSnapshot()).thenReturn(new ObjectMapper().readTree(
                "{\"schemaVersion\":1,\"modules\":{\"marketFundFlow\":{\"tradeDate\":\"2026-09-22\"}}}"));
        MockMvcBuilders.standaloneSetup(new MarketDashboardController(service)).build()
                .perform(get("/market/dashboard/snapshot").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.schemaVersion").value(1))
                .andExpect(jsonPath("$.content.modules.marketFundFlow.tradeDate")
                        .value("2026-09-22"));
    }
}
