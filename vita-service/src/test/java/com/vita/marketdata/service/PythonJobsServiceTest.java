package com.vita.marketdata.service;

import com.vita.core.exception.ServiceException;
import com.vita.marketdata.enums.PythonJobKind;
import com.vita.marketdata.property.StockMonitorProperty;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PythonJobsServiceTest {
    @Test
    void forwardsTokenAndWaitsForFinalResultForEachKind() {
        for (PythonJobKind kind : PythonJobKind.values()) {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("http://python.test/internal/jobs/v1/" + kind.getPath() + "/refresh"))
                    .andExpect(method(HttpMethod.POST)).andExpect(header("X-Internal-Token", "test-only"))
                    .andExpect(content().json("{}"))
                    .andRespond(withSuccess("{\"kind\":\"" + kind.getPath()
                            + "\",\"state\":\"PARTIAL\",\"outcome\":\"partial\","
                            + "\"startedAt\":\"2026-10-03T10:00:00+08:00\","
                            + "\"finishedAt\":\"2026-10-03T10:01:00+08:00\",\"message\":null}",
                            MediaType.APPLICATION_JSON));
            var result = new PythonJobsService(property(), Map.of(kind, builder.build())).refresh(kind);
            assertEquals("PARTIAL", result.state());
            assertNotNull(result.finishedAt());
            server.verify();
        }
        assertEquals(Duration.ofSeconds(1440), PythonJobKind.MARKET.getReadTimeout());
    }

    @Test
    void rejectsMissingConfigurationBeforeAnyRequestAndMapsConflict() {
        assertThrows(ServiceException.class, () -> new PythonJobsService(new StockMonitorProperty(), Map.of())
                .refresh(PythonJobKind.ETF));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.CONFLICT));
        ServiceException exception = assertThrows(ServiceException.class,
                () -> new PythonJobsService(property(), Map.of(PythonJobKind.ETF, builder.build()))
                        .refresh(PythonJobKind.ETF));
        assertEquals(423, exception.getCode());
        server.verify();
    }

    private StockMonitorProperty property() {
        StockMonitorProperty property = new StockMonitorProperty();
        property.setPythonBaseUrl("http://python.test");
        property.setInternalToken("test-only");
        return property;
    }
}
