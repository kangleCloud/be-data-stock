package com.vita.redis.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.redis.config
 * @Author: znk
 * @CreateTime: 2026-03-03  16:33:28
 * @Description: Redisson 自动配置类
 * @Version: 1.0
 */
@Configuration
@ConditionalOnExpression("!${spring.data.redis.enabled:false}")
@EnableAutoConfiguration(exclude = org.redisson.spring.starter.RedissonAutoConfigurationV2.class)
public class RedissonAutoConfiguration {

}
