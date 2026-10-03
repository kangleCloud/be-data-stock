package com.vita.market.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;

/** 指数显示配置提交后通知已有市场 SSE 客户端重新读取公开视图。 */
public class MarketIndexResyncSynchronization implements TransactionSynchronization {
    private final StringRedisTemplate redisTemplate;

    public MarketIndexResyncSynchronization(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void afterCommit() {
        redisTemplate.convertAndSend("stock:market:v1:updates", "{\"resync\":true}");
    }
}
