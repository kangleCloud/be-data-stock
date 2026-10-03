package com.vita.market.service;

import com.vita.market.dto.MarketIndexUpdateRequest;
import com.vita.market.entity.MarketIndexConfig;
import com.vita.market.mapper.MarketIndexConfigMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarketIndexConfigServiceTest {
    @Test
    void movingAnIndexRenumbersAllFiveWithoutDuplicates() {
        MarketIndexConfigMapper mapper = mock(MarketIndexConfigMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        List<MarketIndexConfig> rows = List.of(
                row("sh000001", 1), row("sz399001", 2), row("sh000300", 3),
                row("sz399006", 4), row("sh000688", 5));
        when(mapper.selectForUpdate()).thenReturn(rows);
        MarketIndexConfigService service = new MarketIndexConfigService(mapper, redis);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.update(new MarketIndexUpdateRequest("sz399006", false, 2));
            assertEquals(List.of("sh000001", "sz399006", "sz399001", "sh000300", "sh000688"),
                    rows.stream().sorted(java.util.Comparator.comparing(MarketIndexConfig::getSortOrder))
                            .map(MarketIndexConfig::getCode).toList());
            assertEquals(List.of(1, 2, 3, 4, 5), rows.stream()
                    .map(MarketIndexConfig::getSortOrder).sorted().toList());
            assertFalse(rows.get(3).getEnabled());
            verify(mapper, atLeastOnce()).updateById(any(MarketIndexConfig.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private MarketIndexConfig row(String code, int order) {
        MarketIndexConfig row = new MarketIndexConfig();
        row.setCode(code);
        row.setName(code);
        row.setEnabled(true);
        row.setSortOrder(order);
        return row;
    }
}
