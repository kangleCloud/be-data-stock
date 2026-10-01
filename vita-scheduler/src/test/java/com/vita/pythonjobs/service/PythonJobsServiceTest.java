package com.vita.pythonjobs.service;

import com.vita.core.exception.ServiceException;
import com.vita.pythonjobs.dto.PythonRunResult;
import com.vita.pythonjobs.service.PythonJobsService.JobKind;
import com.vita.stockmonitor.property.StockMonitorProperty;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PythonJobsServiceTest {
    private static final String BASE_URL = "http://python.internal:8000/";
    private static final String TOKEN = "unit-test-token";

    @Test
    void threeKindsWaitForTerminalResponseAndSendInternalToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PythonJobsService service = service(builder.build());
        for (JobKind kind : JobKind.values()) {
            String value = kind.name().toLowerCase();
            server.expect(requestTo("http://python.internal:8000/internal/jobs/v1/" + value + "/refresh"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("X-Internal-Token", TOKEN))
                    .andRespond(withSuccess(result(value, "SUCCEEDED", "published"), MediaType.APPLICATION_JSON));
        }
        for (JobKind kind : JobKind.values()) {
            String value = kind.name().toLowerCase();
            PythonRunResult response = service.refresh(kind);
            assertThat(response.kind()).isEqualTo(value);
            assertThat(response.state()).isEqualTo("SUCCEEDED");
            assertThat(response.outcome()).isEqualTo("published");
        }
        server.verify();
    }

    @Test
    void skippedAndFailedBusinessResultsRemainVisible() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PythonJobsService service = service(builder.build());
        server.expect(requestTo("http://python.internal:8000/internal/jobs/v1/calendar/refresh"))
                .andRespond(withSuccess(result("calendar", "SKIPPED", "throttled"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://python.internal:8000/internal/jobs/v1/market/refresh"))
                .andRespond(withSuccess(result("market", "FAILED", "failed"), MediaType.APPLICATION_JSON));
        assertThat(service.refresh(JobKind.CALENDAR).outcome()).isEqualTo("throttled");
        assertThat(service.refresh(JobKind.MARKET).state()).isEqualTo("FAILED");
        server.verify();
    }

    @Test
    void lockConflictMapsToBusyAndUpstreamFailureToUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PythonJobsService service = service(builder.build());
        server.expect(requestTo("http://python.internal:8000/internal/jobs/v1/market/refresh"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body(result("market", "SKIPPED", "locked")));
        server.expect(requestTo("http://python.internal:8000/internal/jobs/v1/monitor/refresh"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> service.refresh(JobKind.MARKET))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(423);
        assertThatThrownBy(() -> service.refresh(JobKind.MONITOR))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(503);
        server.verify();
    }

    @Test
    void timeoutMapsTo504AndMissingTokenFailsClosed() {
        RestClient timeoutClient = RestClient.builder().requestFactory((uri, method) -> {
            throw new SocketTimeoutException("read timed out");
        }).build();
        assertThatThrownBy(() -> service(timeoutClient).refresh(JobKind.MARKET))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(504);

        StockMonitorProperty missing = property();
        missing.setInternalToken("");
        PythonJobsService noToken = new PythonJobsService(missing, Map.of(
                JobKind.CALENDAR, timeoutClient, JobKind.MARKET, timeoutClient, JobKind.MONITOR, timeoutClient));
        assertThatThrownBy(() -> noToken.refresh(JobKind.CALENDAR))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(503);
    }

    @Test
    void malformedTerminalResponseIsRejected() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PythonJobsService service = service(builder.build());
        server.expect(requestTo("http://python.internal:8000/internal/jobs/v1/monitor/refresh"))
                .andRespond(withSuccess("{\"kind\":\"monitor\",\"state\":\"RUNNING\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.refresh(JobKind.MONITOR))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(503);
        server.verify();
    }

    @Test
    void interruptedRequestRestoresInterruptFlag() {
        RestClient interruptedClient = RestClient.builder().requestFactory((uri, method) -> {
            throw new InterruptedIOException("interrupted");
        }).build();
        try {
            assertThatThrownBy(() -> service(interruptedClient).refresh(JobKind.CALENDAR))
                    .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(503);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private PythonJobsService service(RestClient client) {
        return new PythonJobsService(property(), Map.of(
                JobKind.CALENDAR, client, JobKind.MARKET, client, JobKind.MONITOR, client));
    }

    private StockMonitorProperty property() {
        StockMonitorProperty property = new StockMonitorProperty();
        property.setPythonBaseUrl(BASE_URL);
        property.setInternalToken(TOKEN);
        return property;
    }

    private String result(String kind, String state, String outcome) {
        return "{\"kind\":\"" + kind + "\",\"state\":\"" + state + "\",\"outcome\":\""
                + outcome + "\",\"startedAt\":\"2026-10-01T10:00:00+08:00\","
                + "\"finishedAt\":\"2026-10-01T10:01:00+08:00\",\"message\":\"done\"}";
    }
}
