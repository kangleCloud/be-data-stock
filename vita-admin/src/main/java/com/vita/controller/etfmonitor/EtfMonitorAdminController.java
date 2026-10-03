package com.vita.controller.etfmonitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.etfmonitor.dto.EtfDictionaryPageQuery;
import com.vita.etfmonitor.dto.EtfMonitorDtos;
import com.vita.etfmonitor.dto.EtfProfilePageQuery;
import com.vita.etfmonitor.dto.EtfRefreshResult;
import com.vita.etfmonitor.entity.EtfSymbolDictionary;
import com.vita.etfmonitor.service.EtfMonitorConfigService;
import com.vita.etfmonitor.service.EtfMonitorRefreshService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** ETF 字典、资料与独立启用清单的管理入口。 */
@RestController
public class EtfMonitorAdminController {
    private final EtfMonitorConfigService service;
    private final EtfMonitorRefreshService refreshService;

    public EtfMonitorAdminController(EtfMonitorConfigService service, EtfMonitorRefreshService refreshService) {
        this.service = service;
        this.refreshService = refreshService;
    }

    @GetMapping("/system/etfDictionary/page")
    @SaCheckPermission("system:etf-dictionary:view")
    public CommonResult<PageResponse<EtfSymbolDictionary>> dictionary(@Valid EtfDictionaryPageQuery query) {
        return CommonResult.success(service.pageDictionary(query));
    }

    @GetMapping("/system/etfProfile/page")
    @SaCheckPermission("system:etf-profile:view")
    public CommonResult<PageResponse<EtfMonitorDtos.ProfileRow>> profiles(@Valid EtfProfilePageQuery query) {
        return CommonResult.success(service.pageProfile(query));
    }

    @GetMapping("/system/etfProfile/detail")
    @SaCheckPermission("system:etf-profile:view")
    public CommonResult<EtfMonitorDtos.ProfileDetail> detail(@RequestParam String symbol) {
        return CommonResult.success(service.detail(symbol));
    }

    @GetMapping("/system/etfMonitor/list")
    @SaCheckPermission("system:etf-monitor:view")
    public CommonResult<List<EtfMonitorDtos.AdminEtf>> list() {
        return CommonResult.success(service.list());
    }

    @PostMapping("/system/etfMonitor/enable")
    @SaCheckPermission("system:etf-monitor:update")
    public CommonResult<Boolean> enable(@RequestBody EtfMonitorDtos.EnabledRequest request) {
        service.setEnabled(request.symbol(), request.enabled());
        return CommonResult.success(true);
    }

    @PostMapping("/system/etfMonitor/sort")
    @SaCheckPermission("system:etf-monitor:update")
    public CommonResult<Boolean> sort(@RequestBody EtfMonitorDtos.SortRequest request) {
        service.sort(request.symbols());
        return CommonResult.success(true);
    }

    @PostMapping("/system/etfMonitor/refresh")
    @SaCheckPermission("system:etf-monitor:refresh")
    public CommonResult<EtfRefreshResult> refresh() {
        return CommonResult.success(refreshService.refresh());
    }

    @PostMapping("/system/etfMonitor/allocation/refresh")
    @SaCheckPermission("system:etf-monitor:refresh")
    public CommonResult<EtfRefreshResult> refreshAllocation(@RequestParam String symbol,
                                                              @RequestParam String reportPeriod) {
        return CommonResult.success(refreshService.refreshAllocation(symbol, reportPeriod));
    }
}
