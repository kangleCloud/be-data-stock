package com.vita.marketdata.constant;

import java.time.ZoneId;

/** 三类市场数据共用的时间、清单上限及推送生命周期；股票和 ETF 独立计数。 */
public final class MarketDataConstants {
    public static final String SHANGHAI_ZONE_ID = "Asia/Shanghai";
    public static final ZoneId SHANGHAI = ZoneId.of(SHANGHAI_ZONE_ID);
    public static final int MAX_MONITORS = 10;
    public static final long STREAM_TIMEOUT_MS = 60_000L;
    public static final long HEARTBEAT_SECONDS = 15L;

    private MarketDataConstants() { }
}
