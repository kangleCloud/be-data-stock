package com.vita.marketdata.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.constant.MarketDataConstants;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 每个进程共享的行情推送服务。Redis 通知只负责唤醒，数据始终重新读取快照键。
 */
@Service
public class MarketSnapshotStreamService implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(MarketSnapshotStreamService.class);

    private final MarketSnapshotService snapshotService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final Set<MarketStreamClient> clients = ConcurrentHashMap.newKeySet();
    private final Object broadcastLock = new Object();

    @Autowired
    public MarketSnapshotStreamService(MarketSnapshotService snapshotService, ObjectMapper objectMapper) {
        this(snapshotService, objectMapper, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "market-snapshot-heartbeat");
            thread.setDaemon(true);
            return thread;
        }));
    }

    MarketSnapshotStreamService(MarketSnapshotService snapshotService, ObjectMapper objectMapper,
                                ScheduledExecutorService scheduler) {
        this.snapshotService = snapshotService;
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
    }

    public SseEmitter open() {
        synchronized (broadcastLock) {
            // 与通知读取串行化：建流校验和首帧之间不能漏掉更新。
            String snapshotId = currentSnapshotId();
            SseEmitter emitter = createEmitter();
            MarketStreamClient client = new MarketStreamClient(emitter, snapshotId);
            emitter.onCompletion(() -> close(client, false));
            emitter.onTimeout(() -> close(client, true));
            emitter.onError(error -> close(client, false));
            clients.add(client);
            try {
                synchronized (client) {
                    if (!client.closed.get()) {
                        client.heartbeat = scheduler.scheduleAtFixedRate(() -> heartbeat(client),
                                MarketDataConstants.HEARTBEAT_SECONDS, MarketDataConstants.HEARTBEAT_SECONDS, TimeUnit.SECONDS);
                        client.expiry = scheduler.schedule(() -> close(client, true),
                                MarketDataConstants.STREAM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    }
                }
                ObjectNode ready = objectMapper.createObjectNode();
                putNullable(ready, "snapshotId", snapshotId);
                send(client, "ready", ready);
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
                JsonNode notice = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
                if (notice != null && notice.path("resync").asBoolean(false)) {
                    for (MarketStreamClient client : clients) {
                        resync(client);
                    }
                    return;
                }
                JsonNode snapshot = snapshotService.getSnapshot();
                String nextId = snapshot.path("snapshotId").textValue();
                boolean hasIndices = snapshot.path("modules").has("coreIndices");
                boolean valid = validNotice(notice) && nextId != null
                        && nextId.equals(hasIndices
                        ? snapshotService.publicSnapshotId(notice.path("snapshotId").textValue())
                        : notice.path("snapshotId").textValue());
                for (MarketStreamClient client : clients) {
                    if (valid && !client.resyncRequired && nextId.equals(client.snapshotId)) {
                        continue;
                    }
                    String previousRaw = notice.path("previousSnapshotId").textValue();
                    String previousPublic = hasIndices ? snapshotService.publicSnapshotId(previousRaw) : previousRaw;
                    if (!valid || client.resyncRequired || client.snapshotId == null
                            || !client.snapshotId.equals(previousPublic)) {
                        resync(client);
                        continue;
                    }
                    ObjectNode patch = objectMapper.createObjectNode();
                    patch.put("baseSnapshotId", client.snapshotId);
                    patch.put("snapshotId", nextId);
                    patch.set("generatedAt", snapshot.path("generatedAt"));
                    ObjectNode changed = patch.putObject("modules");
                    for (JsonNode name : notice.path("changedModules")) {
                        changed.set(name.textValue(), snapshot.path("modules").path(name.textValue()));
                    }
                    send(client, "patch", patch);
                    client.snapshotId = nextId;
                }
            } catch (Exception exception) {
                LOG.warn("行情快照通知处理失败，通知客户端重新获取快照", exception);
                for (MarketStreamClient client : clients) {
                    resync(client);
                }
            }
        }
    }

    private String currentSnapshotId() {
        try {
            return snapshotService.getSnapshot().path("snapshotId").textValue();
        } catch (ServiceException exception) {
            if (Integer.valueOf(404).equals(exception.getCode())) {
                return null;
            }
            throw exception;
        }
    }

    private boolean validNotice(JsonNode notice) {
        if (notice == null || !notice.isObject() || notice.path("schemaVersion").asInt(-1) != 1
                || !validId(notice.path("snapshotId").textValue())
                || !notice.path("changedModules").isArray()) {
            return false;
        }
        JsonNode previous = notice.get("previousSnapshotId");
        if (previous == null || !(previous.isNull() || validId(previous.textValue()))) {
            return false;
        }
        Set<String> names = Set.of("industrySectors", "conceptSectors", "marketFundFlow", "coreIndices");
        Set<String> seen = new java.util.HashSet<>();
        for (JsonNode name : notice.path("changedModules")) {
            if (!name.isTextual() || !names.contains(name.textValue()) || !seen.add(name.textValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean validId(String id) {
        return id != null && id.matches("[0-9a-f]{32}");
    }

    private void resync(MarketStreamClient client) {
        client.resyncRequired = true;
        ObjectNode event = objectMapper.createObjectNode();
        send(client, "resync", event);
    }

    private void putNullable(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private void send(MarketStreamClient client, String eventName, JsonNode payload) {
        synchronized (client) {
            if (client.closed.get()) {
                return;
            }
            try {
                client.emitter.send(SseEmitter.event().name(eventName).data(payload.toString()));
            } catch (IOException | IllegalStateException exception) {
                LOG.debug("行情快照流写入失败，关闭连接", exception);
                close(client, false);
            }
        }
    }

    private void heartbeat(MarketStreamClient client) {
        synchronized (client) {
            if (client.closed.get()) {
                return;
            }
            try {
                client.emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException exception) {
                LOG.debug("行情快照流心跳写入失败，关闭连接", exception);
                close(client, false);
            }
        }
    }

    private void close(MarketStreamClient client, boolean complete) {
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
                try {
                    client.emitter.complete();
                } catch (Exception exception) {
                    // 连接已断开时 complete 仍可能触发响应刷新异常；资源已清理，无需再交给 MVC 写响应。
                    LOG.debug("行情快照流已断开，完成响应失败", exception);
                }
            }
        }
    }

    SseEmitter createEmitter() {
        return new SseEmitter(MarketDataConstants.STREAM_TIMEOUT_MS);
    }

    int activeClientCount() {
        return clients.size();
    }

    @PreDestroy
    public void shutdown() {
        for (MarketStreamClient client : clients) {
            close(client, true);
        }
        scheduler.shutdownNow();
    }

}
