package com.vita.etfmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** ETF 监控事件只发送变更股票；版本缺口交给客户端重新读取全量 GET。 */
@Service
public class EtfMonitorStreamService implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(EtfMonitorStreamService.class);
    private static final long STREAM_TIMEOUT_MS = 60_000L;
    private final EtfMonitorDashboardService dashboardService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final Set<EtfMonitorStreamClient> clients = ConcurrentHashMap.newKeySet();
    private final Object broadcastLock = new Object();

    @Autowired
    public EtfMonitorStreamService(EtfMonitorDashboardService dashboardService, ObjectMapper objectMapper) {
        this(dashboardService, objectMapper, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "etf-monitor-heartbeat");
            thread.setDaemon(true);
            return thread;
        }));
    }

    EtfMonitorStreamService(EtfMonitorDashboardService dashboardService, ObjectMapper objectMapper,
                              ScheduledExecutorService scheduler) {
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
    }

    public SseEmitter open() {
        synchronized (broadcastLock) {
            String stateId = dashboardService.dashboard().path("stateId").textValue();
            SseEmitter emitter = createEmitter();
            EtfMonitorStreamClient client = new EtfMonitorStreamClient(emitter, stateId);
            emitter.onCompletion(() -> close(client, false));
            emitter.onTimeout(() -> close(client, true));
            emitter.onError(error -> close(client, false));
            clients.add(client);
            try {
                synchronized (client) {
                    if (!client.closed.get()) {
                        client.heartbeat = scheduler.scheduleAtFixedRate(() -> heartbeat(client),
                                15, 15, TimeUnit.SECONDS);
                        client.expiry = scheduler.schedule(() -> close(client, true),
                                STREAM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    }
                }
                ObjectNode ready = objectMapper.createObjectNode();
                putNullable(ready, "stateId", stateId);
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
                JsonNode dashboard = dashboardService.dashboard();
                boolean valid = validNotice(notice) && dashboard.path("stateId").textValue() != null
                        && dashboard.path("stateId").textValue().equals(notice.path("stateId").textValue());
                for (EtfMonitorStreamClient client : clients) {
                    if (valid && !client.resyncRequired && dashboard.path("stateId").textValue().equals(client.stateId)) {
                        continue;
                    }
                    if (!valid || client.resyncRequired || client.stateId == null
                            || !client.stateId.equals(notice.path("baseStateId").textValue())
                            || notice.path("resync").asBoolean(false)) {
                        resync(client);
                        continue;
                    }
                    ObjectNode patch = objectMapper.createObjectNode();
                    patch.put("baseStateId", client.stateId);
                    patch.put("stateId", dashboard.path("stateId").textValue());
                    var etfs = patch.putArray("etfs");
                    Set<String> changed = new HashSet<>();
                    notice.path("changedSymbols").forEach(node -> changed.add(node.textValue()));
                    for (JsonNode etf : dashboard.path("etfs")) {
                        if (changed.remove(etf.path("symbol").textValue())) {
                            etfs.add(quotePatch(etf));
                        }
                    }
                    if (!changed.isEmpty()) {
                        resync(client);
                        continue;
                    }
                    send(client, "patch", patch);
                    client.stateId = dashboard.path("stateId").textValue();
                }
            } catch (Exception exception) {
                LOG.warn("ETF 通知处理失败，通知客户端重新获取快照", exception);
                for (EtfMonitorStreamClient client : clients) {
                    resync(client);
                }
            }
        }
    }

    private boolean validNotice(JsonNode notice) {
        if (notice == null || !notice.isObject() || !validId(notice.path("stateId").textValue())
                || !notice.path("changedSymbols").isArray()) {
            return false;
        }
        JsonNode base = notice.get("baseStateId");
        if (base == null || !(base.isNull() || validId(base.textValue()))) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        for (JsonNode symbol : notice.path("changedSymbols")) {
            if (!symbol.isTextual() || !symbol.textValue().matches("^(SH|SZ)[0-9]{6}$")
                    || !seen.add(symbol.textValue())) {
                return false;
            }
        }
        return !notice.has("resync") || notice.path("resync").isBoolean();
    }

    JsonNode quotePatch(JsonNode etf) {
        ObjectNode update = etf.deepCopy();
        // 资产配置由 GET 与资料变更 resync 更新，行情采样不重复传输报告。
        update.remove("assetAllocation");
        return update;
    }

    private boolean validId(String id) {
        return id != null && id.matches("[0-9a-f]{32}");
    }

    private void resync(EtfMonitorStreamClient client) {
        client.resyncRequired = true;
        send(client, "resync", objectMapper.createObjectNode());
    }

    private void putNullable(ObjectNode object, String field, String value) {
        if (value == null) {
            object.putNull(field);
        } else {
            object.put(field, value);
        }
    }

    private void send(EtfMonitorStreamClient client, String eventName, JsonNode payload) {
        synchronized (client) {
            if (client.closed.get()) {
                return;
            }
            try {
                client.emitter.send(SseEmitter.event().name(eventName).data(payload.toString()));
            } catch (IOException | IllegalStateException exception) {
                LOG.debug("ETF 监控流写入失败，关闭连接", exception);
                close(client, false);
            }
        }
    }

    private void heartbeat(EtfMonitorStreamClient client) {
        synchronized (client) {
            if (client.closed.get()) {
                return;
            }
            try {
                client.emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException exception) {
                close(client, false);
            }
        }
    }

    private void close(EtfMonitorStreamClient client, boolean complete) {
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
                    LOG.debug("ETF 监控流已断开，完成响应失败", exception);
                }
            }
        }
    }

    SseEmitter createEmitter() {
        return new SseEmitter(STREAM_TIMEOUT_MS);
    }

    @PreDestroy
    public void shutdown() {
        for (EtfMonitorStreamClient client : clients) {
            close(client, true);
        }
        scheduler.shutdownNow();
    }

}
