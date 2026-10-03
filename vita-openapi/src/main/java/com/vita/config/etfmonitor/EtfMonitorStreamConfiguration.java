package com.vita.config.etfmonitor;

import com.vita.etfmonitor.service.EtfMonitorStreamService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class EtfMonitorStreamConfiguration {
    @Bean
    public RedisMessageListenerContainer etfMonitorListenerContainer(
            RedisConnectionFactory connectionFactory, EtfMonitorStreamService streamService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(streamService, new ChannelTopic("stock:etf-monitor:v1:updates"));
        return container;
    }
}
