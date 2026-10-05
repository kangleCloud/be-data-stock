package com.vita.marketdata.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** 多实例之间用带租期的令牌锁保护监控清单和刷新任务。 */
@Component
public class MarketDataRedisLock {
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public MarketDataRedisLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String acquire(String key, Duration lease) {
        String token = UUID.randomUUID().toString();
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, token, lease)) ? token : null;
    }

    public void release(String key, String token) {
        if (token != null) {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(key), token);
        }
    }
}
