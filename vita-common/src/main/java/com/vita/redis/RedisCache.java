package com.vita.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.redis
 * @Author: znk
 * @CreateTime: 2026-03-12  16:11:05
 * @Description: RedisCache Redis缓存类，提供对Redis缓存的操作和管理功能
 * <p>
 * 1. value
 * 2. list
 * 3. set
 * 4. hash
 * 5. zset
 * 6. lua
 * </p>
 * @Version: 1.0
 */
@Component
@SuppressWarnings({"unchecked", "unused"})
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 缓存基本对象
     */
    public <T> void setCacheObject(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本对象并设置过期时间
     */
    public <T> void setCacheObject(String key, T value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 获取缓存对象
     */
    public <T> T getCacheObject(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 原子读取并删除缓存对象，适用于只能消费一次的OAuth state和登录票据。
     *
     * @param key 缓存键
     * @param <T> 缓存值类型
     * @return 删除前的缓存值，不存在时返回空
     */
    public <T> T getAndDeleteCacheObject(String key) {
        return (T) redisTemplate.opsForValue().getAndDelete(key);
    }

    /**
     * setIfAbsent，常用于分布式锁、防重复提交
     */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit timeUnit) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.expire(key, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 删除单个 key
     */
    public boolean deleteObject(String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 批量删除 key
     */
    public long deleteObject(Collection<String> collection) {
        Long count = redisTemplate.delete(collection);
        return count == null ? 0L : count;
    }

    /**
     * 获取匹配的 key
     * 注意：生产大数据量场景建议改用 SCAN
     */
    public Collection<String> keys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys == null ? Collections.emptySet() : keys;
    }

    /**
     * List: 缓存整个列表
     */
    public <T> long setCacheList(String key, List<T> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0L;
        }
        Long count = redisTemplate.opsForList().rightPushAll(key, new ArrayList<>(dataList));
        return count == null ? 0L : count;
    }

    /**
     * List: 追加单个元素
     */
    public <T> void setCacheRightList(String key, T value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * List: 获取全部列表
     */
    public <T> List<T> getCacheList(String key) {
        List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        if (list == null) {
            return Collections.emptyList();
        }
        return (List<T>) list;
    }

    /**
     * Set: 添加单个元素
     */
    public void setCacheSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    /**
     * Set: 批量添加
     */
    public <T> long setCacheSet(String key, Set<T> dataSet) {
        if (dataSet == null || dataSet.isEmpty()) {
            return 0L;
        }
        Long count = redisTemplate.opsForSet().add(key, dataSet.toArray());
        return count == null ? 0L : count;
    }

    /**
     * Set: 获取成员
     */
    public <T> Set<T> getCacheSet(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Collections.emptySet();
        }
        return (Set<T>) members;
    }

    /**
     * Set: 获取大小
     */
    public Long getCacheSetSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    /**
     * Hash: 批量写入
     */
    public <T> void setCacheMap(String key, Map<String, T> dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * Hash: 获取整个 Hash
     */
    public <T> Map<String, T> getCacheMap(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, T> result = new HashMap<>(entries.size());
        entries.forEach((k, v) -> result.put(String.valueOf(k), (T) v));
        return result;
    }

    /**
     * Hash: 写入单个 field
     */
    public <T> void setCacheMapValue(String key, String hKey, T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * Hash: 获取单个 field
     */
    public <T> T getCacheMapValue(String key, String hKey) {
        return (T) redisTemplate.opsForHash().get(key, hKey);
    }

    /**
     * Hash: 批量获取 field
     */
    public <T> List<T> getMultiCacheMapValue(String key, Collection<Object> hKeys) {
        List<Object> list = redisTemplate.opsForHash().multiGet(key, hKeys);
        if (list == null) {
            return Collections.emptyList();
        }
        return (List<T>) list;
    }

    /**
     * Hash: 删除 field
     */
    public void removeMapValueByKey(String key, String hKey) {
        redisTemplate.opsForHash().delete(key, hKey);
    }

    /**
     * Hash: 指定 field 自增
     */
    public Long hashIncrement(String key, String hk, Long value) {
        return redisTemplate.opsForHash().increment(key, hk, value);
    }

    /**
     * String: 自增
     */
    public Long incrObject(String key, long delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("递增因子必须大于 0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * String: 自减
     */
    public Long decrObject(String key, long delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("递减因子必须大于 0");
        }
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * ZSet: 新增成员
     */
    public void zsetAdd(String key, Object member, double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    /**
     * ZSet: 批量新增
     */
    public void zsetBatchAdd(String key, Set<ZSetOperations.TypedTuple<Object>> tuples) {
        if (tuples == null || tuples.isEmpty()) {
            return;
        }
        redisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * ZSet: 获取成员分数
     */
    public Double zsetGet(String key, Object member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * ZSet: 删除成员
     */
    public void zsetRemove(String key, Object member) {
        redisTemplate.opsForZSet().remove(key, member);
    }

    /**
     * ZSet: 获取成员数量
     */
    public Long zsetMemberSize(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return size == null ? 0L : size;
    }

    /**
     * ZSet: 指定成员分数递增
     */
    public Double zsetIncrScore(String key, Object member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    /**
     * ZSet: 获取指定区间
     */
    public <T> Set<T> zsetRange(String key, long start, long end) {
        Set<Object> set = redisTemplate.opsForZSet().range(key, start, end);
        if (set == null) {
            return Collections.emptySet();
        }
        return (Set<T>) set;
    }

    /**
     * 执行 Lua 脚本
     */
    public <T> T execute(RedisScript<T> redisScript, List<String> keys, Object... args) {
        return redisTemplate.execute(redisScript, keys, args);
    }
}
