package com.vita.controller.pythonjobs;

import com.vita.controller.local.LoopbackRequestGuard;
import com.vita.core.CommonResult;
import com.vita.pythonjobs.dto.PythonRunResult;
import com.vita.pythonjobs.service.PythonJobsService;
import com.vita.pythonjobs.service.PythonJobsService.JobKind;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 只允许 scheduler 所在机器直连的 Python 同步任务入口。
 */
@RestController
@RequestMapping("/local/python-jobs/v1")
public class PythonJobsLocalController {
    private final PythonJobsService jobsService;

    public PythonJobsLocalController(PythonJobsService jobsService) {
        this.jobsService = jobsService;
    }

    /**
     * 刷新日历数据，通常用于在 scheduler 机器上定时执行。
     *
     * @param request HTTP 请求对象
     */
    @PostMapping("/calendar/refresh")
    public CommonResult<PythonRunResult> refreshCalendar(HttpServletRequest request) {
        return refresh(request, JobKind.CALENDAR);
    }

    /**
     * 刷新市场数据，通常用于在 scheduler 机器上定时执行。
     *
     * @param request HTTP 请求对象
     */
    @PostMapping("/market/refresh")
    public CommonResult<PythonRunResult> refreshMarket(HttpServletRequest request) {
        return refresh(request, JobKind.MARKET);
    }

    /**
     * 刷新监控数据，通常用于在 scheduler 机器上定时执行。
     *
     * @param request HTTP 请求对象
     */
    @PostMapping("/monitor/refresh")
    public CommonResult<PythonRunResult> refreshMonitor(HttpServletRequest request) {
        return refresh(request, JobKind.MONITOR);
    }

    @PostMapping("/etf/refresh")
    public CommonResult<PythonRunResult> refreshEtf(HttpServletRequest request) {
        return refresh(request, JobKind.ETF);
    }

    /**
     * 刷新指定类型的数据。
     *
     * @param request HTTP 请求对象
     * @param kind    数据类型
     */
    private CommonResult<PythonRunResult> refresh(HttpServletRequest request, JobKind kind) {
        LoopbackRequestGuard.requireLoopback(request);
        return CommonResult.success(jobsService.refresh(kind));
    }
}
