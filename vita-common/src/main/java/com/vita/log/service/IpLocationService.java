package com.vita.log.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.property.VitaProperty;
import com.vita.redis.RedisCache;
import com.vita.utils.web.ip.AddressUtils;
import com.vita.utils.web.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.service
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: IP归属地服务
 * @Version: 1.0
 */
@Component
public class IpLocationService {

    private static final Logger LOG = LoggerFactory.getLogger(IpLocationService.class);

    private static final String CACHE_KEY_PREFIX = "ip_location:";

    private static final long CACHE_TTL_HOURS = 24L;

    private static final int LOOKUP_TIMEOUT_MILLIS = 500;

    private final RedisCache redisCache;

    public IpLocationService(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /**
     * 快速解析归属地，只读缓存，不触发外部调用。
     *
     * @param ip IP地址
     * @return 归属地；未命中时返回 UNKNOWN
     */
    public String resolveFast(String ip) {
        if (!isLookupEligible(ip)) {
            return internalLocation(ip);
        }
        String cachedLocation = getCachedLocation(ip);
        return CharSequenceUtil.isBlank(cachedLocation) ? AddressUtils.UNKNOWN : cachedLocation;
    }

    /**
     * 判断当前记录是否需要异步补全归属地。
     *
     * @param ip IP地址
     * @param currentLocation 当前归属地
     * @return true 表示需要补全
     */
    public boolean needsAsyncEnrichment(String ip, String currentLocation) {
        return isLookupEligible(ip) && Objects.equals(AddressUtils.UNKNOWN, currentLocation);
    }

    /**
     * 异步查询并刷新缓存。
     *
     * @param ip IP地址
     * @return 查询结果；失败时返回 UNKNOWN
     */
    public String resolveAndCache(String ip) {
        if (!isLookupEligible(ip)) {
            return internalLocation(ip);
        }

        String cachedLocation = getCachedLocation(ip);
        if (CharSequenceUtil.isNotBlank(cachedLocation)) {
            return cachedLocation;
        }

        String resolvedLocation = AddressUtils.getRealAddressByIP(ip, LOOKUP_TIMEOUT_MILLIS);
        if (!Objects.equals(AddressUtils.UNKNOWN, resolvedLocation)) {
            try {
                redisCache.setCacheObject(buildCacheKey(ip), resolvedLocation, CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception ex) {
                LOG.warn("缓存IP归属地失败, ip={}", ip, ex);
            }
        }
        return resolvedLocation;
    }

    private String getCachedLocation(String ip) {
        try {
            Object cached = redisCache.getCacheObject(buildCacheKey(ip));
            return cached == null ? null : String.valueOf(cached);
        } catch (Exception ex) {
            LOG.warn("读取IP归属地缓存失败, ip={}", ip, ex);
            return null;
        }
    }

    private boolean isLookupEligible(String ip) {
        return VitaProperty.isAddressEnabled() && IpUtils.isIP(ip) && !IpUtils.internalIp(ip);
    }

    private String internalLocation(String ip) {
        return IpUtils.isIP(ip) && IpUtils.internalIp(ip) ? "内网IP" : AddressUtils.UNKNOWN;
    }

    private String buildCacheKey(String ip) {
        return CACHE_KEY_PREFIX + ip;
    }
}
