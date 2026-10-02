package com.vita.config.stockmonitor;

import com.vita.stockmonitor.service.StockMonitorStreamService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** 个股通知按进程共享 Redis 订阅。 */
@Configuration
public class StockMonitorStreamConfiguration {
    @Bean
    public RedisMessageListenerContainer stockMonitorListenerContainer(
            RedisConnectionFactory connectionFactory, StockMonitorStreamService streamService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(streamService, new ChannelTopic("stock:monitor:v1:updates"));
        return container;
    }
}
