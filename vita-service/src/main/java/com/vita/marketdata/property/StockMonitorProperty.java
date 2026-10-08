package com.vita.marketdata.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 市场数据共用配置入口，保留已有 vita.stock-monitor 绑定前缀。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vita.stock-monitor")
public class StockMonitorProperty {
    private String pythonBaseUrl = "";
    private String internalToken = "";
    private boolean xqEnabled = false;
}
