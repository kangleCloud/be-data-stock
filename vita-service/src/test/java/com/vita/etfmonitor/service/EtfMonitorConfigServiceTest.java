package com.vita.etfmonitor.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.page.PageResponse;
import com.vita.etfmonitor.dto.EtfProfilePageQuery;
import com.vita.etfmonitor.entity.EtfAssetAllocationReport;
import com.vita.etfmonitor.entity.EtfMonitorProfile;
import com.vita.etfmonitor.entity.EtfSymbolDictionary;
import com.vita.etfmonitor.mapper.EtfAssetAllocationReportMapper;
import com.vita.etfmonitor.mapper.EtfMonitorConfigMapper;
import com.vita.etfmonitor.mapper.EtfMonitorProfileMapper;
import com.vita.etfmonitor.mapper.EtfSymbolDictionaryMapper;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.StockMonitorRedisLock;
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
                mock(StockMonitorRedisLock.class), mock(TransactionTemplate.class), property);
    }
}
