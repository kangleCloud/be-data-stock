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
import static org.mockito.Mockito.*;

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
        when(valueOperations.get("stock:market:v2:snapshot")).thenReturn(validSnapshot());

        JsonNode snapshot = service.getSnapshot();

        verify(valueOperations).get("stock:market:v2:snapshot");
        assertThat(snapshot.path("schemaVersion").intValue()).isEqualTo(2);
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
        when(valueOperations.get("stock:market:v2:snapshot")).thenReturn(null);

        assertThatThrownBy(service::getSnapshot)
                .isInstanceOfSatisfying(ServiceException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(GlobalErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void malformedOrUnsupportedSnapshotReturnsBusinessError() {
        for (String invalid : new String[]{"not-json", validSnapshot().replace("\"schemaVersion\":2", "\"schemaVersion\":1"),
                validSnapshot().replace("\"schemaVersion\":2", "\"schemaVersion\":4294967298"),
                validSnapshot().replace("\"conceptTop5\"", "\"otherTop5\""),
                validSnapshot().replace("\"topOutflow\":[]", "\"topOutflow\":null"),
                validSnapshot().replace("\"status\":\"FRESH\"", "\"status\":\"UNKNOWN\"")}) {
            when(valueOperations.get("stock:market:v2:snapshot")).thenReturn(invalid);
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
            when(valueOperations.get("stock:market:v2:snapshot"))
                    .thenReturn(mapper.writeValueAsString(invalid));

            assertThatThrownBy(service::getSnapshot)
                    .isInstanceOfSatisfying(ServiceException.class,
                            exception -> assertThat(exception.getCode())
                                    .isEqualTo(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode()));
        }
    }

    @Test
    void rejectsLegacyAndMalformedTop5Fields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String[] invalid = {
                validSnapshot().replace("\"source\":\"THS\"", "\"source\":\"EM\""),
                validSnapshot().replace("\"period\":\"INTRADAY\"", "\"period\":\"DAILY\""),
                validSnapshot().replace("\"netFlowAmount\":120000000", "\"mainNetInflow\":120000000"),
                validSnapshot().replace("\"sectorType\":\"industry\"", "\"sectorType\":\"concept\""),
                validSnapshot().replace("\"changePercent\":2.5", "\"changePercent\":\"2.5\""),
                validSnapshot().replace("\"netFlowAmount\":120000000", "\"netFlowAmount\":null"),
                validSnapshot().replace("\"sectorName\":\"半导体\"", "\"sectorName\":\"\""),
                validSnapshot().replace("\"topOutflow\":[]", "\"topOutflow\":[],\"unmatchedFundRows\":0"),
                validSnapshot().replace("\"sectorName\":\"半导体\"", "\"sectorCode\":\"BK001\",\"sectorName\":\"半导体\""),
                validSnapshot().replace("\"tradeDateBasis\":\"CALENDAR\"", "\"tradeDateBasis\":\"SOURCE\"")
        };
        for (String raw : invalid) {
            when(valueOperations.get("stock:market:v2:snapshot")).thenReturn(raw);
            assertThatThrownBy(service::getSnapshot)
                    .isInstanceOfSatisfying(ServiceException.class,
                            error -> assertThat(error.getCode())
                                    .isEqualTo(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode()));
        }

        ObjectNode tooMany = (ObjectNode) mapper.readTree(validSnapshot());
        var topRise = (com.fasterxml.jackson.databind.node.ArrayNode) tooMany.path("modules")
                .path("industryTop5").path("data").path("topRise");
        for (int index = 0; index < 5; index++) {
            topRise.add(topRise.get(0).deepCopy());
        }
        when(valueOperations.get("stock:market:v2:snapshot"))
                .thenReturn(mapper.writeValueAsString(tooMany));
        assertThatThrownBy(service::getSnapshot)
                .isInstanceOfSatisfying(ServiceException.class,
                        error -> assertThat(error.getCode())
                                .isEqualTo(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode()));
    }

    private String validSnapshot() {
        return """
                {"schemaVersion":2,"provider":"akshare","generatedAt":"2026-09-23T10:00:00+08:00",
                 "modules":{
                   "industryHeatmap":{"status":"FRESH","tradeDate":"2026-09-23","tradeDateBasis":"CALENDAR","lastSuccessAt":"2026-09-23T10:00:00+08:00","lastAttemptAt":"2026-09-23T10:00:00+08:00","message":null,"data":[{"sectorCode":"BK001","marketCap":123456789,"changePercent":2.5,"turnoverRate":null}]},
                   "conceptHeatmap":{"status":"ERROR","tradeDate":null,"tradeDateBasis":null,"lastSuccessAt":null,"lastAttemptAt":"2026-09-23T10:00:00+08:00","message":"source failed","data":null},
                   "industryTop5":{"status":"FRESH","tradeDate":"2026-09-23","tradeDateBasis":"CALENDAR","lastSuccessAt":"2026-09-23T10:00:00+08:00","lastAttemptAt":"2026-09-23T10:00:00+08:00","message":null,"data":{"source":"THS","period":"INTRADAY","topRise":[{"sectorName":"半导体","sectorType":"industry","changePercent":2.5,"netFlowAmount":120000000}],"topFall":[],"topInflow":[],"topOutflow":[]}},
                   "conceptTop5":{"status":"STALE","tradeDate":"2026-09-22","tradeDateBasis":"CALENDAR","lastSuccessAt":"2026-09-22T15:00:00+08:00","lastAttemptAt":"2026-09-23T10:00:00+08:00","message":"source failed","data":{"source":"THS","period":"INTRADAY","topRise":[],"topFall":[],"topInflow":[{"sectorName":"机器人","sectorType":"concept","changePercent":1.2,"netFlowAmount":98000000}],"topOutflow":[]}},
                   "marketFundFlow":{"status":"FRESH","tradeDate":"2026-09-22","tradeDateBasis":"SOURCE","lastSuccessAt":"2026-09-23T10:00:00+08:00","lastAttemptAt":"2026-09-23T10:00:00+08:00","message":null,"data":{"latest":{"date":"2026-09-22"},"series":[{"date":"2026-09-22"}]}}
                 }}
                """;
    }
}
