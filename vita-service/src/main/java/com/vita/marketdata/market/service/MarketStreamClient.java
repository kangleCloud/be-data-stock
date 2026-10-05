package com.vita.marketdata.market.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** 单条市场 SSE 连接的版本和定时器状态。 */
final class MarketStreamClient {
    final SseEmitter emitter;
    final AtomicBoolean closed = new AtomicBoolean();
    volatile String snapshotId;
    volatile boolean resyncRequired;
    volatile ScheduledFuture<?> heartbeat;
    volatile ScheduledFuture<?> expiry;

    MarketStreamClient(SseEmitter emitter, String snapshotId) {
        this.emitter = emitter;
        this.snapshotId = snapshotId;
    }
}
