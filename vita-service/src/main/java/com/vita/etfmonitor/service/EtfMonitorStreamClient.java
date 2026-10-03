package com.vita.etfmonitor.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** 单条个股 SSE 连接的版本和定时器状态。 */
final class EtfMonitorStreamClient {
    final SseEmitter emitter;
    final AtomicBoolean closed = new AtomicBoolean();
    volatile String stateId;
    volatile boolean resyncRequired;
    volatile ScheduledFuture<?> heartbeat;
    volatile ScheduledFuture<?> expiry;

    EtfMonitorStreamClient(SseEmitter emitter, String stateId) {
        this.emitter = emitter;
        this.stateId = stateId;
    }
}
