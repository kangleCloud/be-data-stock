package com.vita.controller.stockmonitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.dto.StockMonitorPageQuery;
import com.vita.stockmonitor.service.StockMonitorRefreshService;
import com.vita.stockmonitor.service.StockMonitorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 个股监控管理端，仅允许持权用户修改系统级清单。
 */
@RestController
@RequestMapping("/system/stockMonitor")
public class StockMonitorAdminController {
    private final StockMonitorService monitorService;
    private final StockMonitorRefreshService refreshService;

    public StockMonitorAdminController(StockMonitorService monitorService,
                                       StockMonitorRefreshService refreshService) {
        this.monitorService = monitorService;
        this.refreshService = refreshService;
    }

    @GetMapping("/page")
    @SaCheckPermission("system:stock-monitor:view")
    public CommonResult<PageResponse<StockMonitorDtos.AdminStock>> page(@Valid StockMonitorPageQuery query) {
        return CommonResult.success(monitorService.pageMonitor(query));
    }

    @GetMapping("/dictionary")
    @SaCheckPermission("system:stock-monitor:view")
    public CommonResult<List<StockMonitorDtos.DictionaryItem>> dictionary(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return CommonResult.success(monitorService.searchDictionary(keyword, limit));
    }

    @GetMapping("/list")
    @SaCheckPermission("system:stock-monitor:view")
    public CommonResult<List<StockMonitorDtos.AdminStock>> list() {
        return CommonResult.success(monitorService.listAdmin());
    }

    @PostMapping("/enable")
    @SaCheckPermission("system:stock-monitor:update")
    public CommonResult<Boolean> enable(@RequestBody @Valid StockMonitorDtos.EnabledRequest request) {
        monitorService.setEnabled(request.symbol(), request.enabled());
        return CommonResult.success(true);
    }

    @PostMapping("/sort")
    @SaCheckPermission("system:stock-monitor:update")
    public CommonResult<Boolean> sort(@RequestBody @Valid StockMonitorDtos.SortRequest request) {
        monitorService.sort(request.symbols());
        return CommonResult.success(true);
    }

    @PostMapping("/refresh")
    @SaCheckPermission("system:stock-monitor:refresh")
    public CommonResult<StockMonitorDtos.RefreshStatus> refresh() {
        return CommonResult.success(refreshService.refresh());
    }

    @GetMapping("/refresh/status")
    @SaCheckPermission("system:stock-monitor:view")
    public CommonResult<StockMonitorDtos.RefreshStatus> refreshStatus() {
        return CommonResult.success(refreshService.status());
    }
}
