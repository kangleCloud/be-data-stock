package com.vita.config;

import com.vita.etfmonitor.service.EtfMonitorRefreshService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/** 工作日盘后同步 ETF 字典及交易所资料；不自动触发雪球资产配置。 */
@Configuration
public class EtfMonitorDailyJob {
    private final EtfMonitorRefreshService refreshService;

    public EtfMonitorDailyJob(EtfMonitorRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Scheduled(cron = "0 40 16 * * MON-FRI", zone = "Asia/Shanghai")
    public void refresh() {
        refreshService.refresh();
    }
}
