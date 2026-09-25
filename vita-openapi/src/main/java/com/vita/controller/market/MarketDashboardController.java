package com.vita.controller.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.vita.core.CommonResult;
import com.vita.core.CommonStreamResult;
import com.vita.core.StreamEndpoint;
import com.vita.market.service.MarketSnapshotService;
import com.vita.market.service.MarketSnapshotStreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 行情总览接口。
 */
@RestController
@RequestMapping("/market/dashboard")
public class MarketDashboardController {

    private final MarketSnapshotService marketSnapshotService;
    private final MarketSnapshotStreamService streamService;

    public MarketDashboardController(MarketSnapshotService marketSnapshotService,
                                     MarketSnapshotStreamService streamService) {
        this.marketSnapshotService = marketSnapshotService;
        this.streamService = streamService;
    }

    @GetMapping("/snapshot")
    public CommonResult<JsonNode> getSnapshot() {
        return CommonResult.success(marketSnapshotService.getSnapshot());
    }

    @GetMapping("/stream")
    @StreamEndpoint
    public ResponseEntity<SseEmitter> stream() {
        SseEmitter emitter = streamService.open();
        return CommonStreamResult.success(emitter);
    }
}
