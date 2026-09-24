package com.vita.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 每个进程共享的行情推送服务。Redis 通知只负责唤醒，数据始终重新读取快照键。
 */
@Service
public class MarketSnapshotStreamService implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(MarketSnapshotStreamService.class);
    private static final long STREAM_TIMEOUT_MS = 60_000L;
    private static final long HEARTBEAT_SECONDS = 15L;

    private final MarketSnapshotService snapshotService;
    private final ScheduledExecutorService scheduler;
    private final Set<Client> clients = ConcurrentHashMap.newKeySet();
    private final Object broadcastLock = new Object();

    @Autowired
    public MarketSnapshotStreamService(MarketSnapshotService snapshotService) {
        this(snapshotService, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "market-snapshot-heartbeat");
            thread.setDaemon(true);
            return thread;
        }));
    }

    MarketSnapshotStreamService(MarketSnapshotService snapshotService, ScheduledExecutorService scheduler) {
        this.snapshotService = snapshotService;
        this.scheduler = scheduler;
    }

    public SseEmitter open() {
        synchronized (broadcastLock) {
            // 与通知读取串行化：建流校验和首帧之间不能漏掉更新。
            JsonNode snapshot = snapshotService.getSnapshot();
            SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
            Client client = new Client(emitter);
            emitter.onCompletion(() -> close(client, false));
            emitter.onTimeout(() -> close(client, true));
            emitter.onError(error -> close(client, false));
            clients.add(client);
            try {
                synchronized (client) {
                    if (!client.closed.get()) {
                        client.heartbeat = scheduler.scheduleAtFixedRate(() -> heartbeat(client),
                                HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
                        client.expiry = scheduler.schedule(() -> close(client, true),
                                STREAM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    }
                }
                sendSnapshot(client, snapshot);
            } catch (RuntimeException exception) {
                close(client, true);
                throw exception;
            }
            return emitter;
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        synchronized (broadcastLock) {
            if (clients.isEmpty()) {
                return;
            }
            try {
                JsonNode snapshot = snapshotService.getSnapshot();
                for (Client client : clients) {
                    sendSnapshot(client, snapshot);
                }
            } catch (Exception exception) {
                // 已提交响应无法改写成业务错误；关闭连接后由客户端 GET 与重连恢复。
                LOG.warn("行情快照通知处理失败，关闭当前流连接", exception);
                for (Client client : clients) {
                    close(client, true);
                }
            }
        }
    }

    private void sendSnapshot(Client client, JsonNode snapshot) {
        synchronized (client) {
            if (client.closed.get()) {
                return;
            }
            try {
                client.emitter.send(SseEmitter.event().name("snapshot").data(snapshot.toString()));
            } catch (IOException | IllegalStateException exception) {
                LOG.warn("行情快照流写入失败", exception);
                close(client, true);
            }
        }
    }

    private void heartbeat(Client client) {
        synchronized (client) {
            if (client.closed.get()) {
                return;
            }
            try {
                client.emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException exception) {
                LOG.warn("行情快照流心跳写入失败", exception);
                close(client, true);
            }
        }
    }

    private void close(Client client, boolean complete) {
        synchronized (client) {
            if (!client.closed.compareAndSet(false, true)) {
                return;
            }
            clients.remove(client);
            if (client.heartbeat != null) {
                client.heartbeat.cancel(false);
            }
            if (client.expiry != null) {
                client.expiry.cancel(false);
            }
            if (complete) {
                client.emitter.complete();
            }
        }
    }

    int activeClientCount() {
        return clients.size();
    }

    @PreDestroy
    public void shutdown() {
        for (Client client : clients) {
            close(client, true);
        }
        scheduler.shutdownNow();
    }

    private static final class Client {
        private final SseEmitter emitter;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile ScheduledFuture<?> heartbeat;
        private volatile ScheduledFuture<?> expiry;

        private Client(SseEmitter emitter) {
            this.emitter = emitter;
        }
    }
}
