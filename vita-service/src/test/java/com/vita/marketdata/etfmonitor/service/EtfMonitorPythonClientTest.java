package com.vita.marketdata.etfmonitor.service;

import com.vita.core.exception.ServiceException;
import com.vita.marketdata.property.StockMonitorProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EtfMonitorPythonClientTest {
    @Test
    void operationsHaveSeparateBudgetsAndProfilesOnlySendsSymbolsWithToken() {
        RestClient.Builder dictionary = RestClient.builder();
        RestClient.Builder profiles = RestClient.builder();
        RestClient.Builder allocation = RestClient.builder();
        var dictionaryServer = MockRestServiceServer.bindTo(dictionary).build();
        var profilesServer = MockRestServiceServer.bindTo(profiles).build();
        var allocationServer = MockRestServiceServer.bindTo(allocation).build();
        dictionaryServer.expect(requestTo("http://python.test/internal/etf-monitor/v1/dictionary"))
                .andExpect(header("X-Internal-Token", "test-only"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        profilesServer.expect(requestTo("http://python.test/internal/etf-monitor/v1/profiles"))
                .andExpect(header("X-Internal-Token", "test-only"))
                .andExpect(content().json("{\"symbols\":[\"SH510050\"]}", true))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        allocationServer.expect(requestTo("http://python.test/internal/etf-monitor/v1/asset-allocation"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        var client = new EtfMonitorPythonClient(property(), dictionary.build(), profiles.build(), allocation.build());
        client.dictionary();
        client.profiles(Map.of("symbols", List.of("SH510050")));
        client.assetAllocation(Map.of("symbol", "SH510050", "reportPeriod", "20260630"));
        dictionaryServer.verify(); profilesServer.verify(); allocationServer.verify();
        assertEquals(Duration.ofSeconds(60), EtfMonitorPythonClient.DICTIONARY_READ_TIMEOUT);
        assertEquals(Duration.ofSeconds(210), EtfMonitorPythonClient.PROFILES_READ_TIMEOUT);
        assertEquals(Duration.ofSeconds(120), EtfMonitorPythonClient.ALLOCATION_READ_TIMEOUT);
    }

    @Test
    void pythonLockAndIntervalRemainExplicitFailures() {
        for (HttpStatus status : List.of(HttpStatus.CONFLICT, HttpStatus.TOO_MANY_REQUESTS, HttpStatus.BAD_GATEWAY)) {
            RestClient.Builder builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(anything()).andRespond(withStatus(status));
            var client = new EtfMonitorPythonClient(property(), builder.build(), builder.build(), builder.build());
            var exception = assertThrows(ServiceException.class, () -> client.profiles(Map.of("symbols", List.of("SH510050"))));
            assertEquals(status == HttpStatus.CONFLICT ? 423 : status == HttpStatus.TOO_MANY_REQUESTS ? 429 : 503,
                    exception.getCode());
            server.verify();
        }
    }

    @ParameterizedTest
    @CsvSource({"RESOURCE,503,资源不足", "NO_DATA,502,没有该请求报告期", "DISABLED,503,总闸或授权",
            "SOURCE,502,数据源暂不可用", "UNKNOWN,502,同步失败", "LEGACY,502,同步失败", "SOURCE,422,报告期不合法"})
    void allocationFailureUsesOnlyFrozenReasonAndNeverSourceBody(String reason, int status, String message) {
        RestClient.Builder builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        String body = "LEGACY".equals(reason) ? "{\"detail\":\"secret-test\"}"
                : "{\"detail\":{\"reason\":\"" + reason + "\",\"message\":\"secret-test\"}}";
        server.expect(requestTo("http://python.test/internal/etf-monitor/v1/asset-allocation"))
                .andExpect(header("X-Internal-Token", "test-only"))
                .andExpect(content().json("{\"symbol\":\"SH510050\",\"reportPeriod\":\"20260630\"}"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON).body(body));
        var client = new EtfMonitorPythonClient(property(), builder.build(), builder.build(), builder.build());
        var error = assertThrows(ServiceException.class,
                () -> client.assetAllocation(Map.of("symbol", "SH510050", "reportPeriod", "20260630")));
        assertEquals(status == 422 ? 400 : 503, error.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains(message));
        org.junit.jupiter.api.Assertions.assertFalse(error.getMessage().contains("secret-test"));
        server.verify();
    }

    private StockMonitorProperty property() {
        var property = new StockMonitorProperty();
        property.setPythonBaseUrl("http://python.test"); property.setInternalToken("test-only");
        return property;
    }
}
