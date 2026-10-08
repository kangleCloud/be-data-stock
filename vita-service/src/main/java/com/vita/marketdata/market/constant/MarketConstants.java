package com.vita.marketdata.market.constant;

/** V1 Redis 契约，键值与频道不得随包结构重构变更。 */
public final class MarketConstants {
    public static final String SNAPSHOT_KEY = "stock:market:v1:snapshot";
    public static final String UPDATES_CHANNEL = "stock:market:v1:updates";

    private MarketConstants() { }
}
