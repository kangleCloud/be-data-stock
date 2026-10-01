package com.vita.controller.stockmonitor;

import com.vita.controller.local.LoopbackRequestGuard;
import com.vita.core.CommonResult;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.service.StockMonitorRefreshService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅供 scheduler 所在服务器本机手动触发的独立刷新入口。
 */
@RestController
@RequestMapping("/local/stock-monitor/v1")
public class StockMonitorLocalRefreshController {
    private final StockMonitorRefreshService refreshService;

    public StockMonitorLocalRefreshController(StockMonitorRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping("/dictionary/refresh")
    public CommonResult<StockMonitorDtos.RefreshStatus> refreshDictionary(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(refreshService.refreshDictionary());
    }

    @PostMapping("/profiles/refresh")
    public CommonResult<StockMonitorDtos.RefreshStatus> refreshProfiles(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(refreshService.refreshProfiles());
    }

}
