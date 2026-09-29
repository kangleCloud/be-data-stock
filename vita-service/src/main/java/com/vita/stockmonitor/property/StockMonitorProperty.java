package com.vita.stockmonitor.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 个股监控 V1 的 Spring Boot 配置入口。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vita.stock-monitor")
public class StockMonitorProperty {
    private String pythonBaseUrl = "";
    private String internalToken = "";
    private boolean xqEnabled = false;
}
