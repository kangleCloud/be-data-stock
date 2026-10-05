package com.vita.controller.stockmonitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.marketdata.stockmonitor.dto.StockMonitorDtos;
import com.vita.marketdata.stockmonitor.dto.StockProfilePageQuery;
import com.vita.marketdata.stockmonitor.service.StockMonitorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 已同步股票基础资料只读分页。 */
@RestController
@RequestMapping("/system/stockProfile")
public class StockProfileAdminController {
    private final StockMonitorService monitorService;

    public StockProfileAdminController(StockMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/page")
    @SaCheckPermission("system:stock-profile:view")
    public CommonResult<PageResponse<StockMonitorDtos.ProfileStock>> page(@Valid StockProfilePageQuery query) {
        return CommonResult.success(monitorService.pageProfile(query));
    }
}
