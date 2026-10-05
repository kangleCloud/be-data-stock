package com.vita.marketdata.etfmonitor.constant;

/** V1 Redis 契约，键值与频道不得随包结构重构变更。 */
public final class EtfMonitorConstants {
    public static final String ENABLED_KEY = "stock:etf-monitor:v1:enabled";
    public static final String STATE_KEY = "stock:etf-monitor:v1:state-id";
    public static final String SNAPSHOT_KEY = "stock:etf-monitor:v1:snapshot";
    public static final String UPDATES_CHANNEL = "stock:etf-monitor:v1:updates";
    public static final String CONFIG_LOCK = "stock:etf-monitor:v1:config:lock";
    public static final String REFRESH_LOCK = "stock:etf-monitor:v1:refresh:lock";

    private EtfMonitorConstants() { }
}
