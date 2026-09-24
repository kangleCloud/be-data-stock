package com.vita.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.Message;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MarketSnapshotStreamServiceTest {

    private final MarketSnapshotService snapshotService = mock(MarketSnapshotService.class);
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final MarketSnapshotStreamService streamService =
            new MarketSnapshotStreamService(snapshotService, scheduler);

    @Test
    void missingOrInvalidSnapshotFailsBeforeClientIsRegistered() {
        for (int code : new int[]{GlobalErrorCode.NOT_FOUND.getCode(),
                GlobalErrorCode.SERVICE_UNAVAILABLE.getCode()}) {
            reset(snapshotService);
            when(snapshotService.getSnapshot()).thenThrow(new ServiceException(code, "快照不可用"));
            assertThatThrownBy(streamService::open)
                    .isInstanceOfSatisfying(ServiceException.class,
                            error -> assertThat(error.getCode()).isEqualTo(code));
            assertThat(streamService.activeClientCount()).isZero();
        }
    }

    @Test
    void oneNotificationReadsOnceAndBroadcastsToTwoClients() throws Exception {
        when(snapshotService.getSnapshot()).thenReturn(new ObjectMapper().readTree("{\"schemaVersion\":1}"));
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), eq(15L), eq(15L), eq(TimeUnit.SECONDS)))
                .thenReturn(mock(ScheduledFuture.class));
        when(scheduler.schedule(any(Runnable.class), eq(60000L), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(mock(ScheduledFuture.class));

        streamService.open();
        streamService.open();
        assertThat(streamService.activeClientCount()).isEqualTo(2);
        streamService.onMessage(mock(Message.class), null);
        verify(snapshotService, times(3)).getSnapshot();
        verify(scheduler, times(2)).scheduleAtFixedRate(any(Runnable.class), eq(15L), eq(15L), eq(TimeUnit.SECONDS));
        verify(scheduler, times(2)).schedule(any(Runnable.class), eq(60000L), eq(TimeUnit.MILLISECONDS));
        ArgumentCaptor<Runnable> expiryTasks = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(2)).schedule(expiryTasks.capture(), eq(60000L), eq(TimeUnit.MILLISECONDS));
        expiryTasks.getAllValues().get(0).run();
        assertThat(streamService.activeClientCount()).isEqualTo(1);
        streamService.onMessage(mock(Message.class), null);
        verify(snapshotService, times(4)).getSnapshot();
        streamService.shutdown();
        assertThat(streamService.activeClientCount()).isZero();
    }
    @Test
    void invalidSnapshotAfterNotificationClosesOnlyStreamResources() throws Exception {
        when(snapshotService.getSnapshot()).thenReturn(new ObjectMapper().readTree("{\"schemaVersion\":1}"))
                .thenThrow(new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE));
        ScheduledFuture<?> heartbeat = mock(ScheduledFuture.class);
        ScheduledFuture<?> expiry = mock(ScheduledFuture.class);
        doReturn(heartbeat).when(scheduler).scheduleAtFixedRate(
                any(Runnable.class), eq(15L), eq(15L), eq(TimeUnit.SECONDS));
        doReturn(expiry).when(scheduler).schedule(
                any(Runnable.class), eq(60000L), eq(TimeUnit.MILLISECONDS));

        streamService.open();
        streamService.onMessage(mock(Message.class), null);
        assertThat(streamService.activeClientCount()).isZero();
        verify(heartbeat).cancel(false);
        verify(expiry).cancel(false);
        streamService.shutdown();
    }

}
