package com.vita.controller.stockmonitor;

import com.vita.core.CommonResult;
import com.vita.core.CommonStreamResult;
import com.vita.core.StreamEndpoint;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.service.StockMonitorService;
import com.vita.stockmonitor.service.StockMonitorStreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 匿名个股监控大屏，只读取 Redis 报价和有限 MySQL 基础资料。 */
@RestController
@RequestMapping("/stock-monitor/v1")
public class StockMonitorDashboardController {
    private final StockMonitorService monitorService;
    private final StockMonitorStreamService streamService;

    public StockMonitorDashboardController(StockMonitorService monitorService,
                                           StockMonitorStreamService streamService) {
        this.monitorService = monitorService;
        this.streamService = streamService;
    }

    @GetMapping("/dashboard")
    public CommonResult<StockMonitorDtos.Dashboard> dashboard() {
        return CommonResult.success(monitorService.dashboard());
    }

    @GetMapping("/stream")
    @StreamEndpoint
    public ResponseEntity<SseEmitter> stream() {
        return CommonStreamResult.success(streamService.open());
    }
}
