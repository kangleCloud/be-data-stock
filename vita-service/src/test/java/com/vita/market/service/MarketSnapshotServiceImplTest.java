package com.vita.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.market.service.impl.MarketSnapshotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketSnapshotServiceImplTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final MarketSnapshotServiceImpl service =
            new MarketSnapshotServiceImpl(redisTemplate, new ObjectMapper());

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsPythonJsonWithoutChangingUnitsDatesOrNulls() {
        when(valueOperations.get("stock:market:v1:snapshot")).thenReturn(validSnapshot());

        JsonNode snapshot = service.getSnapshot();

        verify(valueOperations).get("stock:market:v1:snapshot");
        assertThat(snapshot.path("schemaVersion").intValue()).isEqualTo(1);
        assertThat(snapshot.path("modules").path("industryHeatmap").path("data").get(0)
                .path("marketCap").longValue()).isEqualTo(123456789L);
        assertThat(snapshot.path("modules").path("industryHeatmap").path("data").get(0)
                .path("changePercent").doubleValue()).isEqualTo(2.5);
        assertThat(snapshot.path("modules").path("industryHeatmap").path("data").get(0)
                .path("turnoverRate").isNull()).isTrue();
        assertThat(snapshot.path("modules").path("marketFundFlow").path("tradeDate").textValue())
                .isEqualTo("2026-09-22");
    }

    @Test
    void missingSnapshotReturnsBusinessNotFound() {
        when(valueOperations.get("stock:market:v1:snapshot")).thenReturn(null);

        assertThatThrownBy(service::getSnapshot)
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(GlobalErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void malformedOrUnsupportedSnapshotReturnsBusinessError() {
        for (String invalid : new String[]{"not-json", validSnapshot().replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
                validSnapshot().replace("\"conceptTop5\"", "\"otherTop5\""),
                validSnapshot().replace("\"topInflow\":[]", "\"topInflow\":null"),
                validSnapshot().replace("\"status\":\"FRESH\"", "\"status\":\"UNKNOWN\"")}) {
            when(valueOperations.get("stock:market:v1:snapshot")).thenReturn(invalid);
            assertThatThrownBy(service::getSnapshot)
                    .isInstanceOfSatisfying(ServiceException.class,
                            exception -> assertThat(exception.getCode())
                                    .isEqualTo(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode()));
        }
    }

    @Test
    void freshAndStaleModulesRequireData() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String moduleName : new String[]{"industryHeatmap", "conceptTop5"}) {
            ObjectNode invalid = (ObjectNode) mapper.readTree(validSnapshot());
            ((ObjectNode) invalid.path("modules").path(moduleName))
                    .putNull("data");
            when(valueOperations.get("stock:market:v1:snapshot"))
                    .thenReturn(mapper.writeValueAsString(invalid));

            assertThatThrownBy(service::getSnapshot)
                    .isInstanceOfSatisfying(ServiceException.class,
                            exception -> assertThat(exception.getCode())
                                    .isEqualTo(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode()));
        }
    }

    private String validSnapshot() {
        return """
                {"schemaVersion":1,"provider":"akshare","generatedAt":"2026-09-23T10:00:00+08:00",
                 "modules":{
                   "industryHeatmap":{"status":"FRESH","tradeDate":"2026-09-23","tradeDateBasis":"CALENDAR","lastSuccessAt":"2026-09-23T10:00:00+08:00","lastAttemptAt":"2026-09-23T10:00:00+08:00","message":null,"data":[{"sectorCode":"BK001","marketCap":123456789,"changePercent":2.5,"turnoverRate":null}]},
                   "conceptHeatmap":{"status":"ERROR","tradeDate":null,"tradeDateBasis":null,"lastSuccessAt":null,"lastAttemptAt":"2026-09-23T10:00:00+08:00","message":"source failed","data":null},
                   "industryTop5":{"status":"FRESH","tradeDate":"2026-09-23","tradeDateBasis":"CALENDAR","lastSuccessAt":"2026-09-23T10:00:00+08:00","lastAttemptAt":"2026-09-23T10:00:00+08:00","message":null,"data":{"topRise":[],"topFall":[],"topInflow":[],"topOutflow":[],"unmatchedFundRows":0}},
                   "conceptTop5":{"status":"STALE","tradeDate":"2026-09-22","tradeDateBasis":"CALENDAR","lastSuccessAt":"2026-09-22T15:00:00+08:00","lastAttemptAt":"2026-09-23T10:00:00+08:00","message":"source failed","data":{"topRise":[],"topFall":[],"topInflow":[],"topOutflow":[],"unmatchedFundRows":0}},
                   "marketFundFlow":{"status":"FRESH","tradeDate":"2026-09-22","tradeDateBasis":"SOURCE","lastSuccessAt":"2026-09-23T10:00:00+08:00","lastAttemptAt":"2026-09-23T10:00:00+08:00","message":null,"data":{"latest":{"date":"2026-09-22"},"series":[{"date":"2026-09-22"}]}}
                 }}
                """;
    }
}
