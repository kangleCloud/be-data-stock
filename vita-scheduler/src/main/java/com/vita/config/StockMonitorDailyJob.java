package com.vita.config;

import com.vita.stockmonitor.service.StockMonitorRefreshService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** 工作日收盘后运行；交易所字典同步与雪球开关由同一刷新服务控制。 */
@Configuration
@EnableScheduling
public class StockMonitorDailyJob {
    private final StockMonitorRefreshService refreshService;

    public StockMonitorDailyJob(StockMonitorRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Shanghai")
    public void refresh() {
        refreshService.refresh();
    }
}
