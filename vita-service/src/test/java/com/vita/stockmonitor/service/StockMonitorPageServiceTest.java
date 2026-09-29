package com.vita.stockmonitor.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.page.PageResponse;
import com.vita.stockmonitor.dto.StockDictionaryPageQuery;
import com.vita.stockmonitor.dto.StockMonitorPageQuery;
import com.vita.stockmonitor.dto.StockProfilePageQuery;
import com.vita.stockmonitor.entity.StockMonitorConfig;
import com.vita.stockmonitor.entity.StockMonitorProfile;
import com.vita.stockmonitor.entity.StockSymbolDictionary;
import com.vita.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.impl.StockMonitorServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockMonitorPageServiceTest {
    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "stock-monitor-test");
        TableInfoHelper.initTableInfo(assistant, StockMonitorConfig.class);
        TableInfoHelper.initTableInfo(assistant, StockSymbolDictionary.class);
        TableInfoHelper.initTableInfo(assistant, StockMonitorProfile.class);
    }

    private final StockSymbolDictionaryMapper dictionaryMapper = mock(StockSymbolDictionaryMapper.class);
    private final StockMonitorConfigMapper configMapper = mock(StockMonitorConfigMapper.class);
    private final StockMonitorProfileMapper profileMapper = mock(StockMonitorProfileMapper.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final StockMonitorServiceImpl service = new StockMonitorServiceImpl(dictionaryMapper, configMapper,
            profileMapper, redisTemplate, mock(StockMonitorRedisLock.class), mock(TransactionTemplate.class),
            new ObjectMapper(), new StockMonitorProperty());

    @Test
    @SuppressWarnings("unchecked")
    void monitorPageKeepsDisabledRowsAndFiltersByNameOrCode() {
        StockMonitorConfig disabled = new StockMonitorConfig();
        disabled.setSymbol("SH600000");
        disabled.setEnabled(false);
        disabled.setSortOrder(2);
        when(configMapper.selectPage(any(StockMonitorPageQuery.class), any(Wrapper.class)))
                .thenReturn(new PageResponse<>(List.of(disabled), 1L));
        when(dictionaryMapper.selectList(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                any(java.util.Collection.class))).thenReturn(List.of(dictionary()));
        when(profileMapper.selectList(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                any(java.util.Collection.class))).thenReturn(List.of());
        StockMonitorPageQuery request = new StockMonitorPageQuery();
        request.setKeyword("浦发");
        request.setEnabled(false);

        var page = service.pageMonitor(request);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).enabled()).isFalse();
        assertThat(page.getList().get(0).name()).isEqualTo("浦发银行");
        assertThat(page.getList().get(0).sortOrder()).isEqualTo(2);
        var wrapper = mockitoWrapper(configMapper);
        assertThat(wrapper.getSqlSegment()).contains("enabled", "stock_symbol_dictionary", "d.code LIKE", "d.name LIKE");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(false, "%浦发%");
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void dictionaryPageFiltersKeywordAndMarketWithDatabaseTotal() {
        when(dictionaryMapper.selectPage(any(StockDictionaryPageQuery.class), any(Wrapper.class)))
                .thenReturn(new PageResponse<>(List.of(dictionary()), 123L));
        StockDictionaryPageQuery request = new StockDictionaryPageQuery();
        request.setKeyword("600000");
        request.setMarket("sh");
        request.setPageSize(10);

        var page = service.pageDictionary(request);

        assertThat(page.getTotal()).isEqualTo(123);
        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).market()).isEqualTo("SH");
        var wrapper = mockitoWrapper(dictionaryMapper);
        assertThat(wrapper.getSqlSegment()).contains("code", "name", "market");
        assertThat(wrapper.getParamNameValuePairs().values()).contains("%600000%", "SH");
        verifyNoInteractions(redisTemplate, configMapper, profileMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void profilePageFiltersIndustryAndReturnsLastSynchronizedFields() {
        StockMonitorProfile profile = new StockMonitorProfile();
        profile.setSymbol("SH600000");
        profile.setIndustry("银行");
        profile.setListingDate("1999-11-10");
        profile.setMarketCap(new BigDecimal("1000000.00"));
        profile.setUpdatedAt("2026-09-28T15:30:00+08:00");
        when(profileMapper.selectPage(any(StockProfilePageQuery.class), any(Wrapper.class)))
                .thenReturn(new PageResponse<>(List.of(profile), 1L));
        when(dictionaryMapper.selectList(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class),
                any(java.util.Collection.class))).thenReturn(List.of(dictionary()));
        StockProfilePageQuery request = new StockProfilePageQuery();
        request.setKeyword("浦发");
        request.setIndustry("银行");

        var page = service.pageProfile(request);

        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).industry()).isEqualTo("银行");
        assertThat(page.getList().get(0).marketCap()).isEqualByComparingTo("1000000.00");
        assertThat(page.getList().get(0).updatedAt()).isEqualTo("2026-09-28T15:30:00+08:00");
        var wrapper = mockitoWrapper(profileMapper);
        assertThat(wrapper.getSqlSegment()).contains("stock_symbol_dictionary", "industry");
        assertThat(wrapper.getParamNameValuePairs().values()).contains("%浦发%", "%银行%");
        verifyNoInteractions(redisTemplate, configMapper);
    }

    private StockSymbolDictionary dictionary() {
        StockSymbolDictionary item = new StockSymbolDictionary();
        item.setSymbol("SH600000");
        item.setCode("600000");
        item.setName("浦发银行");
        item.setMarket("SH");
        return item;
    }

    private LambdaQueryWrapper<?> mockitoWrapper(Object mapper) {
        var invocations = mockingDetails(mapper).getInvocations();
        return (LambdaQueryWrapper<?>) invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("selectPage"))
                .findFirst().orElseThrow().getArgument(1);
    }
}
