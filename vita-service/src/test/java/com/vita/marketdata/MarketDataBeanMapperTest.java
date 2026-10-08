package com.vita.marketdata;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.service.PythonJobsService;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MarketDataBeanMapperTest {
    @Test
    void loadsMovedBeansAndAnnotatedMapperStatementsWithUnchangedPropertyPrefix() throws Exception {
        var scan = new MarketDataMapperScanner();
        scan.addIncludeFilter(new AnnotationTypeFilter(Mapper.class));
        var mappers = scan.findCandidateComponents("com.vita.marketdata");
        assertEquals(8, mappers.size());
        MybatisConfiguration mybatis = new MybatisConfiguration();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "vita.stock-monitor.python-base-url", "http://python.test",
                    "vita.stock-monitor.internal-token", "test-only",
                    "vita.stock-monitor.xq-enabled", "false")));
            ConfigurationPropertiesBindingPostProcessor.register(context);
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));
            context.registerBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class));
            for (BeanDefinition definition : mappers) {
                Class<?> type = Class.forName(definition.getBeanClassName());
                mybatis.addMapper(type);
                registerMock(context, type);
            }
            context.scan("com.vita.marketdata");
            context.refresh();
            assertEquals("http://python.test", context.getBean(StockMonitorProperty.class).getPythonBaseUrl());
            assertFalse(context.getBean(StockMonitorProperty.class).isXqEnabled());
            assertNotNull(context.getBean(PythonJobsService.class));
            assertNotNull(context.getBean(MarketDataRedisLock.class));
            assertNotNull(context.getBean("marketSnapshotServiceImpl"));
            assertNotNull(context.getBean("stockMonitorRefreshService"));
            assertNotNull(context.getBean("etfMonitorRefreshService"));
        }
        assertTrue(mybatis.hasStatement("com.vita.marketdata.market.mapper.MarketIndexConfigMapper.selectForUpdate"));
        assertTrue(mybatis.hasStatement("com.vita.marketdata.etfmonitor.mapper.EtfSymbolDictionaryMapper.upsertBatch"));
        assertTrue(mybatis.hasStatement("com.vita.marketdata.stockmonitor.mapper.StockSymbolDictionaryMapper.upsertBatch"));
    }

    private <T> void registerMock(AnnotationConfigApplicationContext context, Class<T> type) {
        context.registerBean(type, () -> mock(type));
    }
}
