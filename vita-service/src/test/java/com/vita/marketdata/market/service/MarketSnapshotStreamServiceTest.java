package com.vita.marketdata.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.vita.marketdata.support.SseEventCapture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarketSnapshotStreamServiceTest {
    private static final List<String> MODULES = List.of("industrySectors", "conceptSectors", "marketFundFlow", "coreIndices");
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicReference<ObjectNode> current = new AtomicReference<>();
    private MarketSnapshotStreamService stream;
    private List<JsonNode> events;
    private ScheduledExecutorService scheduler;
    private SseEmitter emitter;

    @BeforeEach
    void prepareStream() throws Exception {
        var snapshots = mock(MarketSnapshotService.class);
        when(snapshots.getSnapshot()).thenAnswer(call -> {
            if (current.get() == null) throw new ServiceException(404, "暂无快照");
            return current.get();
        });
        when(snapshots.publicSnapshotId(any())).thenAnswer(call -> {
            String raw = call.getArgument(0);
            return raw == null ? null : id(Integer.parseInt(raw, 16) + 100);
        });
        current.set(snapshot(1, true));
        scheduler = mock(ScheduledExecutorService.class);
        stream = spy(new MarketSnapshotStreamService(snapshots, json, scheduler));
        emitter = mock(SseEmitter.class);
        events = attach(emitter, json);
        doReturn(emitter).when(stream).createEmitter();
    }

    @AfterEach
    void closeStream() {
        stream.shutdown();
    }

    @Test
    void multipleCommitsWithinOneRoundPatchOnlyCompletedModulesUsingPublicVersions() {
        stream.open();
        assertEquals(id(101), events.get(0).path("data").path("snapshotId").asText());
        for (int i = 0; i < MODULES.size(); i++) {
            String changed = MODULES.get(i);
            var next = current.get().deepCopy();
            next.put("snapshotId", id(i + 102));
            next.put("generatedAt", "2026-09-28T15:10:0" + i + "+08:00");
            ((ObjectNode) next.path("modules").path(changed)).put("lastAttemptAt", next.path("generatedAt").asText());
            current.set(next);
            var before = next.deepCopy();
            stream.onMessage(message(notice(i + 1, i + 2, changed)), null);
            var patch = events.get(i + 1);
            assertEquals("patch", patch.path("event").asText());
            var data = patch.path("data");
            assertEquals(id(i + 101), data.path("baseSnapshotId").asText());
            assertEquals(id(i + 102), data.path("snapshotId").asText());
            assertEquals(next.path("generatedAt"), data.path("generatedAt"));
            assertEquals(1, data.path("modules").size());
            assertEquals(next.path("modules").path(changed), data.path("modules").path(changed));
            assertEquals(before, next, "消费通知不修改来源时间、历史状态或资金点");
        }
        stream.onMessage(message(notice(4, 5, "coreIndices")), null);
        assertEquals(5, events.size());
    }

    @Test
    void legacySnapshotWithoutIndicesUsesRawVersions() {
        current.set(snapshot(1, false));
        stream.open();
        current.set(snapshot(2, false));
        stream.onMessage(message(notice(1, 2, "industrySectors")), null);
        assertEquals(id(1), events.get(1).path("data").path("baseSnapshotId").asText());
        assertEquals(id(2), events.get(1).path("data").path("snapshotId").asText());
    }

    @Test
    void fundsChannelBetweenQuoteModulesKeepsContinuousPublicVersionsAndUntouchedModules() {
        stream.open();
        String[] completed = {"coreIndices", "marketFundFlow", "industrySectors"};
        var originalConcept = current.get().path("modules").path("conceptSectors").deepCopy();
        for (int i = 0; i < completed.length; i++) {
            var merged = current.get().deepCopy();
            merged.put("snapshotId", id(102 + i));
            var module = (ObjectNode) merged.path("modules").path(completed[i]);
            module.put("lastAttemptAt", "2026-10-10T10:0" + i + ":00+08:00");
            current.set(merged);
            stream.onMessage(message(notice(i + 1, i + 2, completed[i])), null);
            var patch = events.get(i + 1).path("data");
            assertEquals(id(101 + i), patch.path("baseSnapshotId").asText());
            assertEquals(id(102 + i), patch.path("snapshotId").asText());
            assertEquals(1, patch.path("modules").size());
            assertEquals(module, patch.path("modules").path(completed[i]));
            assertEquals(originalConcept, merged.path("modules").path("conceptSectors"));
        }
        assertEquals(4, events.size());
        assertEquals("2026-10-10T10:01:00+08:00",
                current.get().path("modules").path("marketFundFlow").path("lastAttemptAt").asText());
    }

    @Test
    void redisAheadOfNoticeRequiresResyncUntilReconnect() {
        stream.open();
        current.set(snapshot(3, true));
        stream.onMessage(message(notice(1, 2, "industrySectors")), null);
        assertResync(1);
        stream.onMessage(message(notice(1, 3, "coreIndices")), null);
        assertResync(2);
    }

    @Test
    void outOfOrderNoticeAfterSuccessfulPatchRequiresResync() {
        stream.open();
        current.set(snapshot(2, true));
        stream.onMessage(message(notice(1, 2, "industrySectors")), null);
        assertEquals("patch", events.get(1).path("event").asText());
        stream.onMessage(message(notice(0, 1, "conceptSectors")), null);
        assertResync(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gap", "missingBase", "missingNext", "duplicateModule", "unknownModule", "invalidId"})
    void brokenVersionOrModuleNoticeRequiresResync(String scenario) {
        stream.open();
        current.set(snapshot(3, true));
        var notice = notice(1, 3, "marketFundFlow");
        switch (scenario) {
            case "gap" -> notice.put("previousSnapshotId", id(2));
            case "missingBase" -> notice.remove("previousSnapshotId");
            case "missingNext" -> notice.remove("snapshotId");
            case "duplicateModule" -> notice.withArray("changedModules").add("marketFundFlow");
            case "unknownModule" -> notice.withArray("changedModules").add("unknown");
            case "invalidId" -> notice.put("snapshotId", "bad-id");
            default -> fail("未知测试场景");
        }
        stream.onMessage(message(notice), null);
        assertResync(1);
    }

    @Test
    void firstSnapshotAfterEmptyReadyRequiresFullRead() {
        current.set(null);
        stream.open();
        assertTrue(events.get(0).path("data").path("snapshotId").isNull());
        current.set(snapshot(2, true));
        var notice = notice(1, 2, "marketFundFlow");
        notice.putNull("previousSnapshotId");
        stream.onMessage(message(notice), null);
        assertResync(1);
    }

    @Test
    void explicitResyncAtSameVersionIsDelivered() {
        stream.open();
        var notice = notice(1, 1);
        notice.put("resync", true);
        stream.onMessage(message(notice), null);
        assertResync(1);
    }

    @Test
    void scheduledSixtySecondExpiryCompletesConnectionWithoutErrorEvent() {
        stream.open();
        var expiry = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(expiry.capture(), eq(60_000L), eq(TimeUnit.MILLISECONDS));
        expiry.getValue().run();
        expiry.getValue().run();
        verify(emitter, times(1)).complete();
        assertEquals(0, stream.activeClientCount());
        assertEquals(1, events.size(), "到期只结束连接，不发送错误事件");
    }

    @Test
    void concurrentModuleNoticesWithLatestRedisVersionCannotInventPatch() throws Exception {
        stream.open();
        current.set(snapshot(3, true));
        var workers = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var industry = workers.submit(() -> { start.await(); stream.onMessage(message(notice(1, 2, "industrySectors")), null); return null; });
            var index = workers.submit(() -> { start.await(); stream.onMessage(message(notice(2, 3, "coreIndices")), null); return null; });
            start.countDown();
            industry.get(5, TimeUnit.SECONDS);
            index.get(5, TimeUnit.SECONDS);
            assertEquals(3, events.size());
            assertResync(1);
            assertResync(2);
        } finally {
            workers.shutdownNow();
        }
    }

    private void assertResync(int index) {
        assertEquals("resync", events.get(index).path("event").asText());
        assertTrue(events.get(index).path("data").isEmpty());
    }

    private ObjectNode notice(int previous, int next, String... modules) {
        var notice = json.createObjectNode().put("schemaVersion", 1)
                .put("previousSnapshotId", id(previous)).put("snapshotId", id(next));
        var changed = notice.putArray("changedModules");
        for (String module : modules) changed.add(module);
        return notice;
    }

    private ObjectNode snapshot(int version, boolean indices) {
        var snapshot = json.createObjectNode().put("snapshotId", id(indices ? version + 100 : version))
                .put("generatedAt", "2026-09-28T15:10:00+08:00");
        var modules = snapshot.putObject("modules");
        for (String name : MODULES) {
            if (!indices && "coreIndices".equals(name)) continue;
            var module = modules.putObject(name).put("status", "STALE").put("tradeDate", "2026-09-25")
                    .put("lastAttemptAt", "2026-09-28T10:00:00+08:00")
                    .put("lastSuccessAt", "2026-09-25T15:05:00+08:00");
            module.putObject("data").put("sourceTime", "2026-09-25T15:01:00+08:00");
        }
        return snapshot;
    }
}
