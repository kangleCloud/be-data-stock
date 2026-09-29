package com.vita.controller.stockmonitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.page.PageResponse;
import com.vita.stockmonitor.dto.StockDictionaryCreateDto;
import com.vita.stockmonitor.dto.StockDictionaryPageQuery;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.service.StockMonitorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 交易所股票字典只读分页。 */
@RestController
@RequestMapping("/system/stockDictionary")
public class StockDictionaryAdminController {
    private final StockMonitorService monitorService;

    public StockDictionaryAdminController(StockMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/page")
    @SaCheckPermission("system:stock-dictionary:view")
    public CommonResult<PageResponse<StockMonitorDtos.DictionaryItem>> page(@Valid StockDictionaryPageQuery query) {
        return CommonResult.success(monitorService.pageDictionary(query));
    }

    @PostMapping("/add")
    @SaCheckPermission("system:stock-dictionary:add")
    public CommonResult<StockMonitorDtos.DictionaryItem> add(@RequestBody @Valid StockDictionaryCreateDto request) {
        return CommonResult.success(monitorService.addDictionaryStock(request));
    }
}
