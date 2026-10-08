package com.vita.marketdata.stockmonitor.constant;

/** V1 Redis 契约，键值与频道不得随包结构重构变更。 */
public final class StockMonitorConstants {
    public static final String ENABLED_KEY = "stock:monitor:v1:enabled";
    public static final String CONFIG_LOCK = "stock:monitor:v1:config:lock";
    public static final String LAST_TRADE_DATE_KEY = "stock:monitor:v1:lastTradeDate";
    public static final String STATE_ID_KEY = "stock:monitor:v1:state-id";
    public static final String UPDATES_CHANNEL = "stock:monitor:v1:updates";
    public static final String REFRESH_LOCK = "stock:monitor:v1:refresh:lock";
    public static final String STATUS_KEY = "stock:monitor:v1:refresh:status";
    public static final String DICTIONARY_INTERVAL_KEY = "stock:monitor:v1:refresh:dictionary:interval";
    public static final String PROFILES_INTERVAL_KEY = "stock:monitor:v1:refresh:profiles:interval";
    public static final String QUOTE_PREFIX = "stock:monitor:v1:quote:";
    public static final String SERIES_PREFIX = "stock:monitor:v1:series:";
    public static final String FUND_SERIES_PREFIX = "stock:monitor:v1:fund-series:";

    private StockMonitorConstants() { }
}
