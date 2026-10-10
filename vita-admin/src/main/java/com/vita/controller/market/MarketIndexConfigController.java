package com.vita.controller.market;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.marketdata.market.dto.MarketIndexConfigDto;
import com.vita.marketdata.market.dto.MarketIndexUpdateRequest;
import com.vita.marketdata.market.service.MarketIndexConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/indexConfig")
public class MarketIndexConfigController {
    private final MarketIndexConfigService service;

    public MarketIndexConfigController(MarketIndexConfigService service) {
        this.service = service;
    }

    @GetMapping("/list")
    @SaCheckPermission("system:index-config:view")
    public CommonResult<List<MarketIndexConfigDto>> list() {
        return CommonResult.success(service.list());
    }

    @PostMapping("/update")
    @SaCheckPermission("system:index-config:update")
    public CommonResult<MarketIndexConfigDto> update(@RequestBody @Valid MarketIndexUpdateRequest request) {
        return CommonResult.success(service.update(request));
    }
}
