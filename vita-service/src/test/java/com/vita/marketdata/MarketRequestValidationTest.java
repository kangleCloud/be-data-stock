package com.vita.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.marketdata.etfmonitor.dto.EtfDictionaryPageQuery;
import com.vita.marketdata.etfmonitor.dto.EtfMonitorDtos;
import com.vita.marketdata.etfmonitor.dto.EtfProfilePageQuery;
import com.vita.marketdata.market.dto.MarketIndexUpdateRequest;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.stockmonitor.dto.*;
import com.vita.marketdata.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.marketdata.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.marketdata.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.marketdata.stockmonitor.service.impl.StockMonitorServiceImpl;
import com.vita.marketdata.support.MarketDataRedisLock;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MarketRequestValidationTest {
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @AfterEach
    void closeValidator() {
        factory.close();
    }

    @Test
    void normalUnicodeAndMarkupLikeStockNameRemainPlainTextInStorageAndJson() throws Exception {
        String name = "中国平安 & 收益<5> \"报价\" <script>文本</script>";
        var request = new StockDictionaryCreateDto("SH", "600000", name);
        assertTrue(validator.validate(request).isEmpty());
        var dictionary = mock(StockSymbolDictionaryMapper.class);
        var service = new StockMonitorServiceImpl(dictionary, mock(StockMonitorConfigMapper.class),
                mock(StockMonitorProfileMapper.class), mock(StringRedisTemplate.class), mock(MarketDataRedisLock.class),
                mock(TransactionTemplate.class), new ObjectMapper(), new StockMonitorProperty());
        var result = service.addDictionaryStock(request);
        assertEquals(name, result.name());
        var capture = org.mockito.ArgumentCaptor.forClass(com.vita.marketdata.stockmonitor.entity.StockSymbolDictionary.class);
        verify(dictionary).insert(capture.capture());
        assertEquals(name, capture.getValue().getName());
        var json = new ObjectMapper();
        assertEquals(name, json.readTree(json.writeValueAsString(result)).path("name").asText());
        assertFalse(validator.validate(new StockDictionaryCreateDto("SH", "600000", "股".repeat(101))).isEmpty());
    }

    @Test
    void enableSortAndIndexRequestsEnforceExistingDomainBounds() {
        assertTrue(validator.validate(new StockMonitorDtos.EnabledRequest("BJ430001", true)).isEmpty());
        assertFalse(validator.validate(new StockMonitorDtos.EnabledRequest("bad", null)).isEmpty());
        assertFalse(validator.validate(new EtfMonitorDtos.EnabledRequest("BJ430001", true)).isEmpty());
        assertFalse(validator.validate(new StockMonitorDtos.SortRequest(List.of("javascript:bad"))).isEmpty());
        assertFalse(validator.validate(new EtfMonitorDtos.SortRequest(java.util.Collections.nCopies(11, "SH510050"))).isEmpty());
        assertTrue(validator.validate(new StockMonitorDtos.SortRequest(List.of())).isEmpty());
        assertTrue(validator.validate(new MarketIndexUpdateRequest("sh000001", true, 5)).isEmpty());
        assertFalse(validator.validate(new MarketIndexUpdateRequest("sh000002", true, 6)).isEmpty());
    }

    @Test
    void queryLengthAndMarketEnumsDoNotRewriteNormalText() {
        String text = "中文 & <收益>";
        var stock = new StockDictionaryPageQuery(); stock.setKeyword(text); stock.setMarket("sh");
        assertTrue(validator.validate(stock).isEmpty()); assertEquals(text, stock.getKeyword());
        stock.setKeyword("a".repeat(101)); assertFalse(validator.validate(stock).isEmpty());
        var monitor = new StockMonitorPageQuery(); monitor.setKeyword("a".repeat(101));
        assertFalse(validator.validate(monitor).isEmpty());
        var profile = new StockProfilePageQuery(); profile.setIndustry("a".repeat(101));
        assertFalse(validator.validate(profile).isEmpty());
        var etf = new EtfDictionaryPageQuery(); etf.setMarket("BJ");
        assertFalse(validator.validate(etf).isEmpty());
        var etfProfile = new EtfProfilePageQuery(); etfProfile.setTrackingIndexCode("a".repeat(33));
        assertFalse(validator.validate(etfProfile).isEmpty());
    }
}
