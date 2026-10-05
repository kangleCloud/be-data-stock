package com.vita.controller.marketdata;

import com.vita.controller.local.LoopbackRequestGuard;
import com.vita.core.CommonResult;
import com.vita.marketdata.dto.PythonRunResult;
import com.vita.marketdata.enums.PythonJobKind;
import com.vita.marketdata.etfmonitor.dto.EtfRefreshResult;
import com.vita.marketdata.etfmonitor.service.EtfMonitorRefreshService;
import com.vita.marketdata.service.PythonJobsService;
import com.vita.marketdata.stockmonitor.dto.StockMonitorDtos;
import com.vita.marketdata.stockmonitor.service.StockMonitorRefreshService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅供 scheduler 所在服务器真实回环直连，八项业务独立触发并同步返回结果。
 */
@RestController
@RequestMapping("/local/market-data/v1")
public class MarketDataRefreshController {
    private final StockMonitorRefreshService stocks;
    private final EtfMonitorRefreshService etfs;
    private final PythonJobsService jobs;

    public MarketDataRefreshController(StockMonitorRefreshService stocks,
                                       EtfMonitorRefreshService etfs, PythonJobsService jobs) {
        this.stocks = stocks;
        this.etfs = etfs;
        this.jobs = jobs;
    }

    /**
     * 刷新股票字典，通常用于在 scheduler 机器上定时执行。
     *
     * @param request
     * @return
     */
    @PostMapping("/stock/dictionary/refresh")
    public CommonResult<StockMonitorDtos.RefreshStatus> refreshStockDictionary(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(stocks.refreshDictionary());
    }

    /**
     * 刷新股票基础信息，通常用于在 scheduler 机器上定时执行。
     *
     * @param request
     * @return
     */
    @PostMapping("/stock/profiles/refresh")
    public CommonResult<StockMonitorDtos.RefreshStatus> refreshStockProfiles(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(stocks.refreshProfiles());
    }

    /**
     * 刷新交易日历，通常用于在 scheduler 机器上定时执行。
     *
     * @param request
     * @return
     */
    @PostMapping("/calendar/refresh")
    public CommonResult<PythonRunResult> refreshCalendar(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(jobs.refresh(PythonJobKind.CALENDAR));
    }

    /**
     * 刷新市场信息，通常用于在 scheduler 机器上定时执行。
     *
     * @param request
     * @return
     */
    @PostMapping("/market/refresh")
    public CommonResult<PythonRunResult> refreshMarket(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(jobs.refresh(PythonJobKind.MARKET));
    }

    /**
     * 刷新股票行情及曲线，通常用于在 scheduler 机器上定时执行。
     *
     * @param request
     * @return
     */
    @PostMapping("/stock/quotes/refresh")
    public CommonResult<PythonRunResult> refreshStockQuotes(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(jobs.refresh(PythonJobKind.MONITOR));
    }

    @PostMapping("/etf/dictionary/refresh")
    public CommonResult<EtfRefreshResult> refreshEtfDictionary(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(etfs.refreshDictionary());
    }

    /**
     * 刷新 ETF 基础信息，通常用于在 scheduler 机器上定时执行。
     *
     * @param request
     * @return
     */
    @PostMapping("/etf/profiles/refresh")
    public CommonResult<EtfRefreshResult> refreshEtfProfiles(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(etfs.refreshProfiles());
    }

    /**
     * 刷新 ETF 行情及曲线，通常用于在 scheduler 机器上定时执行。
     */

    @PostMapping("/etf/quotes/refresh")
    public CommonResult<PythonRunResult> refreshEtfQuotes(HttpServletRequest request) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(jobs.refresh(PythonJobKind.ETF));
    }
}
