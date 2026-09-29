package com.vita.controller.stockmonitor;

import com.vita.core.CommonResult;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.StockMonitorRefreshService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 调度服务内部触发入口，使用与 Python 服务一致的服务令牌。 */
@RestController
@RequestMapping("/internal/stock-monitor/v1")
public class StockMonitorRefreshController {
    private final StockMonitorRefreshService refreshService;
    private final String internalToken;

    public StockMonitorRefreshController(StockMonitorRefreshService refreshService,
                                         StockMonitorProperty property) {
        this.refreshService = refreshService;
        this.internalToken = property.getInternalToken();
    }

    @PostMapping("/refresh")
    public CommonResult<StockMonitorDtos.RefreshStatus> refresh(
            @RequestHeader(value = "X-Internal-Token", required = false) String suppliedToken) {
        if (internalToken.isBlank() || suppliedToken == null || !MessageDigest.isEqual(
                internalToken.getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        return CommonResult.success(refreshService.refresh());
    }
}
