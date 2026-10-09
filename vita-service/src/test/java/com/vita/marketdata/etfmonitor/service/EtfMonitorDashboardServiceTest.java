package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EtfMonitorDashboardServiceTest {
    @Test
    void stateChangesDuringReadRetryAgainstLatestVersion() {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:etf-monitor:v1:enabled")).thenReturn("[]");
        when(values.get("stock:etf-monitor:v1:state-id")).thenReturn(
                "11111111111111111111111111111111", "22222222222222222222222222222222",
                "33333333333333333333333333333333", "33333333333333333333333333333333");
        var service = new EtfMonitorDashboardService(redis, new ObjectMapper(),
                mock(EtfMonitorProfileMapper.class), mock(EtfAssetAllocationReportMapper.class),
                new StockMonitorProperty());
        assertEquals("33333333333333333333333333333333", service.dashboard().path("stateId").asText());
        verify(values, times(2)).get("stock:etf-monitor:v1:enabled");
    }

    @Test
    void repeatedStateChangesRejectMixedDashboardAfterBoundedRetry() {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:etf-monitor:v1:enabled")).thenReturn("[]");
        when(values.get("stock:etf-monitor:v1:state-id")).thenReturn(
                "11111111111111111111111111111111", "22222222222222222222222222222222",
                "33333333333333333333333333333333", "44444444444444444444444444444444");
        var service = new EtfMonitorDashboardService(redis, new ObjectMapper(),
                mock(EtfMonitorProfileMapper.class), mock(EtfAssetAllocationReportMapper.class),
                new StockMonitorProperty());
        assertEquals(503, assertThrows(ServiceException.class, service::dashboard).getCode());
        verify(values, times(2)).get("stock:etf-monitor:v1:enabled");
    }

    @Test
    void rejectsSnapshotWithUnapprovedSource() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:etf-monitor:v1:state-id"))
                .thenReturn("11111111111111111111111111111111");
        when(values.get("stock:etf-monitor:v1:enabled")).thenReturn("[]");
        when(values.get("stock:etf-monitor:v1:snapshot"))
                .thenReturn("{\"schemaVersion\":1,\"source\":\"EASTMONEY\","
                        + "\"tradeDate\":\"2026-09-30\",\"items\":[]}");
        var service = new EtfMonitorDashboardService(redis, new ObjectMapper(),
                mock(EtfMonitorProfileMapper.class), mock(EtfAssetAllocationReportMapper.class),
                new StockMonitorProperty());
        assertThrows(ServiceException.class, service::dashboard);
    }

    @Test
    void currentDaySeriesWithoutQuoteIsDelayedAndDoesNotThrow() {
        String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:etf-monitor:v1:state-id"))
                .thenReturn("11111111111111111111111111111111");
        when(values.get("stock:etf-monitor:v1:enabled"))
                .thenReturn("[{\"symbol\":\"SH510050\",\"code\":\"510050\",\"name\":\"50ETF\",\"market\":\"SH\"}]");
        when(values.get("stock:etf-monitor:v1:snapshot"))
                .thenReturn("{\"schemaVersion\":1,\"source\":\"AKShare.fund_etf_category_sina\","
                        + "\"tradeDate\":\"" + today + "\",\"items\":[{"
                        + "\"symbol\":\"SH510050\",\"quote\":null,\"priceSeries\":[{"
                        + "\"collectedAt\":\"" + today + "T10:00:00+08:00\",\"price\":2.5}]}]}");
        StockMonitorProperty property = new StockMonitorProperty();
        var dashboard = new EtfMonitorDashboardService(redis, new ObjectMapper(),
                mock(EtfMonitorProfileMapper.class), mock(EtfAssetAllocationReportMapper.class),
                property).dashboard();
        var etf = dashboard.path("etfs").get(0);
        assertTrue(etf.path("quote").isNull());
        assertEquals(1, etf.path("series").size());
        assertEquals(today, etf.path("effectiveTradeDate").asText());
        assertEquals("DELAYED", etf.path("dataStatus").asText());
    }

    @Test
    void xqOffKeepsSinaTradingQuoteAndActualPriceSeries() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:etf-monitor:v1:state-id"))
                .thenReturn("11111111111111111111111111111111");
        when(values.get("stock:etf-monitor:v1:enabled"))
                .thenReturn("[{\"symbol\":\"SH510050\",\"code\":\"510050\",\"name\":\"50ETF\",\"market\":\"SH\"}]");
        when(values.get("stock:etf-monitor:v1:snapshot"))
                .thenReturn("{\"schemaVersion\":1,\"source\":\"AKShare.fund_etf_category_sina\","
                        + "\"tradeDate\":\"2026-09-30\",\"items\":[{"
                        + "\"symbol\":\"SH510050\",\"quote\":{\"source\":\"SINA_ETF\","
                        + "\"sourceTime\":null,\"collectedAt\":\"2026-09-30T10:00:00+08:00\","
                        + "\"tradeDate\":\"2026-09-30\",\"price\":2.5,\"status\":\"FRESH\"},"
                        + "\"priceSeries\":[{\"collectedAt\":\"2026-09-30T10:00:00+08:00\",\"price\":2.5}]}]}");
        EtfMonitorProfileMapper profiles = mock(EtfMonitorProfileMapper.class);
        EtfAssetAllocationReportMapper allocations = mock(EtfAssetAllocationReportMapper.class);
        StockMonitorProperty property = new StockMonitorProperty();
        property.setXqEnabled(false);
        ObjectNode dashboard = new EtfMonitorDashboardService(redis, new ObjectMapper(), profiles,
                allocations, property).dashboard();
        var etf = dashboard.path("etfs").get(0);
        assertFalse(dashboard.path("xqEnabled").asBoolean());
        assertEquals(2.5, etf.path("quote").path("price").asDouble());
        assertEquals(1, etf.path("series").size());
        assertEquals("NO_RELIABLE_SOURCE", etf.path("fundFlowStatus").asText());
        assertTrue(etf.path("assetAllocation").isNull());
        verify(allocations, never()).selectOne(org.mockito.ArgumentMatchers.any(
                com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test
    void thsProfileExposesDistinctFieldsAndOldExchangeProfileIsEmpty() {
        EtfMonitorProfile profile = new EtfMonitorProfile();
        profile.setSource("THS"); profile.setFullName("上证50ETF");
        profile.setFundType("股票型"); profile.setEtfType("股票ETF");
        profile.setFundManager("经理甲"); profile.setManager("管理公司甲");
        profile.setEstablishedDate(LocalDate.of(2019, 12, 1));
        profile.setPerformanceBenchmark("某指数收益率");
        profile.setProfileUpdatedAt(LocalDateTime.of(2026, 10, 3, 10, 1));
        var value = dashboardProfile(profile);
        assertEquals("THS", value.path("source").asText());
        assertEquals("股票型", value.path("fundType").asText());
        assertEquals("股票ETF", value.path("etfType").asText());
        assertEquals("经理甲", value.path("fundManager").asText());
        assertEquals("管理公司甲", value.path("manager").asText());
        assertEquals("2019-12-01", value.path("establishedDate").asText());
        assertEquals(java.time.OffsetDateTime.parse("2026-10-03T10:01:00+08:00"),
                java.time.OffsetDateTime.parse(value.path("updatedAt").asText()));
        assertTrue(value.path("listingDate").isNull());
        assertTrue(value.path("trackingIndexCode").isNull());
        profile.setSource(null);
        profile.setListingDate(LocalDate.of(2020, 1, 1));
        var old = dashboardProfile(profile);
        assertTrue(old.path("source").isNull());
        assertTrue(old.path("listingDate").isNull());
        assertTrue(old.path("updatedAt").isNull());
        assertTrue(old.path("fullName").isNull());
    }

    private com.fasterxml.jackson.databind.JsonNode dashboardProfile(EtfMonitorProfile profile) {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("stock:etf-monitor:v1:enabled"))
                .thenReturn("[{\"symbol\":\"SH510050\",\"code\":\"510050\",\"name\":\"50ETF\",\"market\":\"SH\"}]");
        when(values.get("stock:etf-monitor:v1:snapshot"))
                .thenReturn("{\"schemaVersion\":1,\"source\":\"AKShare.fund_etf_category_sina\",\"tradeDate\":\"2026-10-03\",\"items\":[]}");
        var profiles = mock(EtfMonitorProfileMapper.class);
        when(profiles.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class), eq("SH510050")))
                .thenReturn(profile);
        return new EtfMonitorDashboardService(redis, new ObjectMapper(), profiles,
                mock(EtfAssetAllocationReportMapper.class), new StockMonitorProperty())
                .dashboard().path("etfs").get(0).path("profile");
    }
}
