package com.vita.controller.market;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.databind.JsonNode;
import com.vita.core.CommonResult;
import com.vita.market.service.MarketSnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行情总览接口。
 */
@RestController
@RequestMapping("/market/dashboard")
public class MarketDashboardController {

    private final MarketSnapshotService marketSnapshotService;

    public MarketDashboardController(MarketSnapshotService marketSnapshotService) {
        this.marketSnapshotService = marketSnapshotService;
    }

    @GetMapping("/snapshot")
    @SaCheckPermission("market:dashboard:view")
    public CommonResult<JsonNode> getSnapshot() {
        return CommonResult.success(marketSnapshotService.getSnapshot());
    }
}
