package com.vita.controller.etfmonitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.vita.core.CommonResult;
import com.vita.core.CommonStreamResult;
import com.vita.core.StreamEndpoint;
import com.vita.etfmonitor.service.EtfMonitorDashboardService;
import com.vita.etfmonitor.service.EtfMonitorStreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/etf-monitor/v1")
public class EtfMonitorDashboardController {
    private final EtfMonitorDashboardService dashboardService;
    private final EtfMonitorStreamService streamService;

    public EtfMonitorDashboardController(EtfMonitorDashboardService dashboardService,
                                         EtfMonitorStreamService streamService) {
        this.dashboardService = dashboardService;
        this.streamService = streamService;
    }

    @GetMapping("/dashboard")
    public CommonResult<JsonNode> dashboard() {
        return CommonResult.success(dashboardService.dashboard());
    }

    @GetMapping("/stream")
    @StreamEndpoint
    public ResponseEntity<SseEmitter> stream() {
        return CommonStreamResult.success(streamService.open());
    }
}
