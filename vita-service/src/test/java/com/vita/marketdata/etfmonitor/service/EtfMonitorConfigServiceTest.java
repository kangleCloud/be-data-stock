package com.vita.marketdata.etfmonitor.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.page.PageResponse;
import com.vita.marketdata.etfmonitor.dto.EtfProfilePageQuery;
import com.vita.marketdata.etfmonitor.entity.EtfAssetAllocationReport;
import com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile;
import com.vita.marketdata.etfmonitor.entity.EtfSymbolDictionary;
import com.vita.marketdata.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorConfigMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.marketdata.etfmonitor.mapper.EtfSymbolDictionaryMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtfMonitorConfigServiceTest {
    @Test
    void profilePageJoinsDictionaryIdentityAfterDatabasePagination() {
        EtfSymbolDictionaryMapper dictionary = mock(EtfSymbolDictionaryMapper.class);
        EtfMonitorProfileMapper profiles = mock(EtfMonitorProfileMapper.class);
        EtfProfilePageQuery query = new EtfProfilePageQuery();
        EtfMonitorProfile profile = new EtfMonitorProfile();
        profile.setSymbol("SH510050");
        profile.setExchange("SSE");
        profile.setSource("THS");
        profile.setFundType("股票型");
        profile.setFullName("上证50ETF");
        profile.setProfileUpdatedAt(LocalDateTime.of(2026, 10, 3, 10, 1));
        when(profiles.selectPage(eq(query), any(Wrapper.class)))
                .thenReturn(new PageResponse<>(List.of(profile), 1L));
        EtfSymbolDictionary item = dictionary();
        when(dictionary.selectList(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class), anyList()))
                .thenReturn(List.of(item));
        var page = service(dictionary, profiles, mock(EtfAssetAllocationReportMapper.class), false)
                .pageProfile(query);
        assertEquals(1L, page.getTotal());
        assertEquals("510050", page.getList().get(0).code());
        assertEquals("50ETF", page.getList().get(0).name());
        assertEquals("SH", page.getList().get(0).market());
        assertEquals("股票型", page.getList().get(0).fundType());
        assertEquals("上证50ETF", page.getList().get(0).fullName());
        assertEquals("THS", page.getList().get(0).source());
        assertEquals(java.time.OffsetDateTime.parse("2026-10-03T10:01:00+08:00"),
                java.time.OffsetDateTime.parse(page.getList().get(0).updatedAt()));
    }

    @Test
    void detailParsesCategoryArrayAndAddsShanghaiOffset() {
        EtfSymbolDictionaryMapper dictionary = mock(EtfSymbolDictionaryMapper.class);
        EtfMonitorProfileMapper profiles = mock(EtfMonitorProfileMapper.class);
        EtfAssetAllocationReportMapper allocations = mock(EtfAssetAllocationReportMapper.class);
        when(dictionary.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                eq("SH510050"))).thenReturn(dictionary());
        EtfAssetAllocationReport report = new EtfAssetAllocationReport();
        report.setSymbol("SH510050");
        report.setRequestedReportPeriod(LocalDate.of(2026, 6, 30));
        report.setSource("XQ_DANJUAN");
        report.setCollectedAt(LocalDateTime.of(2026, 9, 30, 10, 0));
        report.setCategoriesJson("[{\"category\":\"股票\",\"percent\":91.2}]");
        when(allocations.selectOne(any(Wrapper.class))).thenReturn(report);
        Object allocation = service(dictionary, profiles, allocations, true)
                .detail("SH510050").assetAllocation();
        JsonNode node = new ObjectMapper().valueToTree(allocation);
        assertEquals(1, node.path("categories").size());
        assertEquals("股票", node.path("categories").get(0).path("category").asText());
        assertTrue(node.path("collectedAt").asText().endsWith("+08:00"));
        assertTrue(node.path("categoriesJson").isMissingNode());
    }

    private EtfSymbolDictionary dictionary() {
        EtfSymbolDictionary item = new EtfSymbolDictionary();
        item.setSymbol("SH510050");
        item.setCode("510050");
        item.setName("50ETF");
        item.setMarket("SH");
        return item;
    }

    private EtfMonitorConfigService service(EtfSymbolDictionaryMapper dictionary,
                                            EtfMonitorProfileMapper profiles,
                                            EtfAssetAllocationReportMapper allocations,
                                            boolean xqEnabled) {
        StockMonitorProperty property = new StockMonitorProperty();
        property.setXqEnabled(xqEnabled);
        return new EtfMonitorConfigService(dictionary, mock(EtfMonitorConfigMapper.class), profiles,
                allocations, mock(StringRedisTemplate.class), new ObjectMapper(),
                mock(MarketDataRedisLock.class), mock(TransactionTemplate.class), property);
    }

    @Test
    void detailProfileUsesPublicFieldsAndAcquisitionTimeWithXqDisabled() {
        var dictionary = mock(EtfSymbolDictionaryMapper.class);
        var profiles = mock(EtfMonitorProfileMapper.class);
        var allocations = mock(EtfAssetAllocationReportMapper.class);
        when(dictionary.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class), eq("SH510050")))
                .thenReturn(dictionary());
        var row = new EtfMonitorProfile();
        row.setSymbol("SH510050"); row.setSource("THS"); row.setFullName("上证50ETF");
        row.setFundManager("经理甲"); row.setManager("管理公司甲");
        row.setProfileUpdatedAt(LocalDateTime.of(2026, 10, 3, 10, 1));
        when(profiles.selectOne(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class), eq("SH510050")))
                .thenReturn(row);
        var result = service(dictionary, profiles, allocations, false).detail("SH510050").profile();
        assertEquals("THS", result.source());
        assertEquals("经理甲", result.fundManager());
        assertEquals("管理公司甲", result.manager());
        assertTrue(result.updatedAt().endsWith("+08:00"));
        org.mockito.Mockito.verifyNoInteractions(allocations);
    }
}
