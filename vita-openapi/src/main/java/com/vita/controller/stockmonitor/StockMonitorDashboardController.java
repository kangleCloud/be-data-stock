package com.vita.controller.stockmonitor;

import com.vita.core.CommonResult;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.service.StockMonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 匿名个股监控大屏，只读取 Redis 报价和有限 MySQL 基础资料。 */
@RestController
@RequestMapping("/stock-monitor/v1")
public class StockMonitorDashboardController {
    private final StockMonitorService monitorService;

    public StockMonitorDashboardController(StockMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/dashboard")
    public CommonResult<StockMonitorDtos.Dashboard> dashboard() {
        return CommonResult.success(monitorService.dashboard());
    }
}
