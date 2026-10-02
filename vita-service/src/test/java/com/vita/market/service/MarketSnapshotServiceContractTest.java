package com.vita.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.ServiceException;
import com.vita.market.service.impl.MarketSnapshotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MarketSnapshotServiceContractTest {
    private static final String SNAPSHOT = """
            {"schemaVersion":1,"provider":"akshare","generatedAt":"2026-09-30T10:02:00+08:00","modules":{
              "industrySectors":{"status":"FRESH","tradeDate":"2026-09-30","tradeDateBasis":"CALENDAR",
                "lastSuccessAt":"2026-09-30T10:01:58+08:00","lastAttemptAt":"2026-09-30T10:01:58+08:00","message":null,
                "data":{"source":"THS","period":"INTRADAY","items":[{"code":null,"name":"银行","type":"industry",
                  "indexValue":1234.5,"changePct":2.31,"inflow":1000000,"outflow":700000,"netAmount":300000,
                  "netFlowRate":17.647,"companyCount":42,"leader":"浦发银行","leaderChangePct":3.1,"leaderPrice":10.2}]}},
              "conceptSectors":{"status":"STALE","tradeDate":"2026-09-30","tradeDateBasis":"CALENDAR",
                "lastSuccessAt":"2026-09-30T09:59:58+08:00","lastAttemptAt":"2026-09-30T10:01:58+08:00","message":"采集失败",
                "data":{"source":"THS","period":"INTRADAY","items":[{"code":"885001","name":"人工智能","type":"concept",
                  "indexValue":null,"changePct":null,"inflow":null,"outflow":null,"netAmount":null,
                  "netFlowRate":null,"companyCount":null,"leader":null,"leaderChangePct":null,"leaderPrice":null}]}},
              "marketFundFlow":{"status":"FRESH","tradeDate":"2026-09-30","tradeDateBasis":"CALENDAR",
                "lastSuccessAt":"2026-09-30T10:02:00+08:00","lastAttemptAt":"2026-09-30T10:02:00+08:00","message":null,
                "data":{"source":"THS_INDIVIDUAL_AGGREGATE","latest":{"collectedAt":"2026-09-30T10:02:00+08:00",
                  "inflow":1000000,"outflow":700000,"netAmount":300000,"riseCount":2500,"fallCount":2100,
                  "flatCount":100,"stockCount":4700},"series":[{"collectedAt":"2026-09-30T10:00:00+08:00",
                  "inflow":900000,"outflow":800000,"netAmount":100000},{"collectedAt":"2026-09-30T10:02:00+08:00",
                  "inflow":1000000,"outflow":700000,"netAmount":300000}]}}
            }}
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private ValueOperations<String, String> values;
    private MarketSnapshotServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new MarketSnapshotServiceImpl(redis, mapper);
    }

    @Test
    void returnsCompleteSnapshotWithLegacyVersionAndReconciliationFlag() throws Exception {
        when(values.get(anyString())).thenReturn(SNAPSHOT);

        var result = service.getSnapshot();
        assertThat(result.path("snapshotId").isNull()).isTrue();
        assertThat(result.path("modules").path("marketFundFlow").path("data")
                .path("reconciledFromLegacy").booleanValue()).isFalse();
        assertThat(result.path("modules").path("marketFundFlow").path("data")
                .path("series")).isEqualTo(fixture().path("modules").path("marketFundFlow")
                .path("data").path("series"));
        verify(values, times(1)).get("stock:market:v1:snapshot");
    }

    @Test
    void acceptsErrorWithoutDataAndStaleWithLastSuccessData() throws Exception {
        ObjectNode snapshot = fixture();
        ObjectNode industry = module(snapshot, "industrySectors");
        industry.put("status", "ERROR");
        industry.putNull("tradeDate");
        industry.putNull("lastSuccessAt");
        industry.putNull("data");
        when(values.get(anyString())).thenReturn(snapshot.toString());

        assertThat(service.getSnapshot().path("modules").path("conceptSectors").path("status").asText())
                .isEqualTo("STALE");
        assertThat(service.getSnapshot().path("modules").path("industrySectors").path("data").isNull()).isTrue();
    }

    @Test
    void rejectsMissingFlowsAndCountMismatch() throws Exception {
        ObjectNode snapshot = fixture();
        ObjectNode data = (ObjectNode) module(snapshot, "marketFundFlow").path("data");
        ObjectNode latest = (ObjectNode) data.path("latest");
        latest.putNull("inflow");
        assertUnavailable(snapshot);

        snapshot = fixture();
        latest = (ObjectNode) module(snapshot, "marketFundFlow").path("data").path("latest");
        latest.put("stockCount", 4701);
        assertUnavailable(snapshot);
    }

    @Test
    void correctsLegacyNetAmountsInLatestAndSeries() throws Exception {
        ObjectNode snapshot = fixture();
        ObjectNode data = (ObjectNode) module(snapshot, "marketFundFlow").path("data");
        ((ObjectNode) data.path("latest")).put("netAmount", 1);
        ((ObjectNode) data.path("series").get(0)).put("netAmount", 2);
        when(values.get(anyString())).thenReturn(snapshot.toString());

        var corrected = service.getSnapshot().path("modules").path("marketFundFlow").path("data");
        assertThat(corrected.path("reconciledFromLegacy").booleanValue()).isTrue();
        assertThat(corrected.path("latest").path("netAmount").decimalValue()).isEqualByComparingTo("300000");
        assertThat(corrected.path("series").get(0).path("netAmount").decimalValue())
                .isEqualByComparingTo("100000");
    }

    @Test
    void acceptsPythonHexSnapshotId() throws Exception {
        ObjectNode snapshot = fixture();
        snapshot.put("snapshotId", "0123456789abcdef0123456789abcdef");
        ((ObjectNode) module(snapshot, "marketFundFlow").path("data"))
                .put("reconciledFromLegacy", false);
        when(values.get(anyString())).thenReturn(snapshot.toString());

        assertThat(service.getSnapshot().path("snapshotId").textValue())
                .isEqualTo("0123456789abcdef0123456789abcdef");
    }

    @Test
    void companyCountRequiresIntegerJsonRatherThanFloatingPointJson() throws Exception {
        ObjectNode snapshot = fixture();
        ObjectNode item = (ObjectNode) module(snapshot, "industrySectors").path("data").path("items").get(0);
        item.put("companyCount", 105);
        when(values.get(anyString())).thenReturn(snapshot.toString());
        assertThat(service.getSnapshot().path("modules").path("industrySectors")
                .path("data").path("items").get(0).path("companyCount").intValue()).isEqualTo(105);

        item.put("companyCount", 105.0);
        assertUnavailable(snapshot);
    }

    @Test
    void rejectsOldRankingsAndLegacyMarketSource() throws Exception {
        ObjectNode snapshot = fixture();
        ObjectNode modules = (ObjectNode) snapshot.path("modules");
        modules.set("industryTop5", modules.remove("industrySectors"));
        assertUnavailable(snapshot);

        snapshot = fixture();
        ((ObjectNode) module(snapshot, "marketFundFlow").path("data")).put("source", "EASTMONEY");
        assertUnavailable(snapshot);
    }

    @Test
    void rejectsBadNumbersMissingFieldsAndWrongSectorType() throws Exception {
        ObjectNode snapshot = fixture();
        ObjectNode item = (ObjectNode) module(snapshot, "industrySectors").path("data").path("items").get(0);
        item.put("changePct", "2.31");
        assertUnavailable(snapshot);

        snapshot = fixture();
        item = (ObjectNode) module(snapshot, "industrySectors").path("data").path("items").get(0);
        item.remove("netFlowRate");
        assertUnavailable(snapshot);

        snapshot = fixture();
        item = (ObjectNode) module(snapshot, "industrySectors").path("data").path("items").get(0);
        item.put("type", "concept");
        assertUnavailable(snapshot);

        snapshot = fixture();
        item = (ObjectNode) module(snapshot, "industrySectors").path("data").path("items").get(0);
        item.put("code", " ");
        assertUnavailable(snapshot);

        when(values.get(anyString())).thenReturn(SNAPSHOT.replace("\"changePct\":2.31", "\"changePct\":1e999"));
        assertThatThrownBy(service::getSnapshot).isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(503);
    }

    @Test
    void rejectsSeriesFromAnotherDayOrDuplicatedSamplingTime() throws Exception {
        ObjectNode snapshot = fixture();
        ObjectNode point = (ObjectNode) module(snapshot, "marketFundFlow").path("data").path("series").get(0);
        point.put("collectedAt", "2026-09-29T10:00:00+08:00");
        assertUnavailable(snapshot);

        snapshot = fixture();
        point = (ObjectNode) module(snapshot, "marketFundFlow").path("data").path("series").get(1);
        point.put("collectedAt", "2026-09-30T10:00:00+08:00");
        assertUnavailable(snapshot);
    }

    @Test
    void missingSnapshotIs404AndMalformedSnapshotIs503() {
        when(values.get(anyString())).thenReturn(null);
        assertThatThrownBy(service::getSnapshot).isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(404);

        when(values.get(anyString())).thenReturn("{bad json");
        assertThatThrownBy(service::getSnapshot).isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(503);
    }

    private ObjectNode fixture() throws Exception {
        return (ObjectNode) mapper.readTree(SNAPSHOT);
    }

    private ObjectNode module(ObjectNode snapshot, String name) {
        return (ObjectNode) snapshot.path("modules").path(name);
    }

    private void assertUnavailable(ObjectNode snapshot) {
        when(values.get(anyString())).thenReturn(snapshot.toString());
        assertThatThrownBy(service::getSnapshot).isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(503);
    }
}
