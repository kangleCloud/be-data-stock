package com.vita.market.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** 进程级 Redis 订阅，浏览器连接不会创建独立订阅。 */
@Configuration
public class MarketSnapshotStreamConfiguration {

    @Bean
    public RedisMessageListenerContainer marketSnapshotListenerContainer(
            RedisConnectionFactory connectionFactory, MarketSnapshotStreamService streamService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(streamService, new ChannelTopic("stock:market:v1:updates"));
        return container;
    }
}
