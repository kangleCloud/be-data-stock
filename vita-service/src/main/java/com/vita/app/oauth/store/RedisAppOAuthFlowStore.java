package com.vita.app.oauth.store;

import com.alibaba.fastjson2.JSON;
import com.vita.app.oauth.model.AppOAuthState;
import com.vita.app.oauth.model.AppOAuthTicket;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.redis.RedisCache;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.oauth.store
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: Redis OAuth临时状态存储实现
 * @Version: 1.0
 */
@Component
public class RedisAppOAuthFlowStore {

    private static final String STATE_KEY_PREFIX = "app:oauth:state:";
    private static final String TICKET_KEY_PREFIX = "app:oauth:ticket:";

    private final RedisCache redisCache;

    public RedisAppOAuthFlowStore(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public void saveState(String state, AppOAuthState value, Duration timeout) {
        redisCache.setCacheObject(STATE_KEY_PREFIX + state,
                JSON.toJSONString(value), requirePositiveSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * state通过Redis GETDEL原子消费，重复回调即使并发到达也只有一个请求可以继续换取平台令牌。
     *
     * @param state OAuth state
     * @return 一次性流程状态
     */
    public AppOAuthState consumeState(String state) {
        String value = redisCache.getAndDeleteCacheObject(STATE_KEY_PREFIX + state);
        if (value == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "OAuth状态无效或已过期");
        }
        return JSON.parseObject(value, AppOAuthState.class);
    }

    public void saveTicket(String ticket, AppOAuthTicket value, Duration timeout) {
        redisCache.setCacheObject(TICKET_KEY_PREFIX + ticket,
                JSON.toJSONString(value), requirePositiveSeconds(timeout), TimeUnit.SECONDS);
    }

    /**
     * 登录票据只能兑换一次，避免前端回调URL中的短期票据被重放。
     *
     * @param ticket OAuth票据
     * @return 一次性票据内容
     */
    public AppOAuthTicket consumeTicket(String ticket) {
        String value = redisCache.getAndDeleteCacheObject(TICKET_KEY_PREFIX + ticket);
        if (value == null) {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(), "OAuth票据无效或已过期");
        }
        return JSON.parseObject(value, AppOAuthTicket.class);
    }

    private long requirePositiveSeconds(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), "OAuth临时状态有效期配置错误");
        }
        return Math.max(1L, duration.toSeconds());
    }
}
