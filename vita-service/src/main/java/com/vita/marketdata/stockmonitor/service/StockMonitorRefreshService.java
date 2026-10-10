package com.vita.marketdata.stockmonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.constant.MarketDataConstants;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.stockmonitor.constant.StockMonitorConstants;
import com.vita.marketdata.stockmonitor.dto.StockMonitorDtos;
import com.vita.marketdata.stockmonitor.entity.StockMonitorConfig;
import com.vita.marketdata.stockmonitor.entity.StockMonitorProfile;
import com.vita.marketdata.stockmonitor.entity.StockSymbolDictionary;
import com.vita.marketdata.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.marketdata.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.marketdata.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.marketdata.support.MarketDataRedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 交易所字典与最多 10 只选中股票资料的整体刷新。 */
@Service
public class StockMonitorRefreshService {
    private static final Logger LOG = LoggerFactory.getLogger(StockMonitorRefreshService.class);

    private final StockMonitorPythonClient pythonClient;
    private final StockSymbolDictionaryMapper dictionaryMapper;
    private final StockMonitorConfigMapper configMapper;
    private final StockMonitorProfileMapper profileMapper;
    private final StockMonitorService monitorService;
    private final MarketDataRedisLock redisLock;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean xqEnabled;
    private final TransactionTemplate transactions;

    private enum RefreshScope { ALL, DICTIONARY, PROFILES }

    public StockMonitorRefreshService(StockMonitorPythonClient pythonClient,
                                      StockSymbolDictionaryMapper dictionaryMapper,
                                      StockMonitorConfigMapper configMapper,
                                      StockMonitorProfileMapper profileMapper,
                                      StockMonitorService monitorService,
                                      MarketDataRedisLock redisLock,
                                      StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper,
                                      StockMonitorProperty property, TransactionTemplate transactions) {
        this.pythonClient = pythonClient;
        this.dictionaryMapper = dictionaryMapper;
        this.configMapper = configMapper;
        this.profileMapper = profileMapper;
        this.monitorService = monitorService;
        this.redisLock = redisLock;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.xqEnabled = property.isXqEnabled();
        this.transactions = transactions;
    }

    public StockMonitorDtos.RefreshStatus refresh() {
        return refresh(RefreshScope.ALL, CollectionMode.AUTO);
    }

    public StockMonitorDtos.RefreshStatus refreshDictionary() {
        return refreshDictionary(CollectionMode.AUTO);
    }

    public StockMonitorDtos.RefreshStatus refreshDictionary(CollectionMode mode) {
        return refresh(RefreshScope.DICTIONARY, mode);
    }

    public StockMonitorDtos.RefreshStatus refreshProfiles() {
        return refreshProfiles(CollectionMode.AUTO);
    }

    public StockMonitorDtos.RefreshStatus refreshProfiles(CollectionMode mode) {
        if (!xqEnabled) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "雪球采集总闸已关闭");
        }
        return refresh(RefreshScope.PROFILES, mode);
    }

    private StockMonitorDtos.RefreshStatus refresh(RefreshScope scope, CollectionMode mode) {
        // 本机手动请求不占用自动准入锁、触发间隔，也不读写自动任务状态。
        if (mode == CollectionMode.MANUAL) {
            return execute(scope, mode);
        }
        String token = redisLock.acquire(StockMonitorConstants.REFRESH_LOCK, Duration.ofMinutes(15));
        if (token == null) {
            StockMonitorDtos.RefreshStatus current = status();
            return new StockMonitorDtos.RefreshStatus(false, current.status(),
                    current.startedAt(), current.finishedAt(), "刷新任务正在执行");
        }
        try {
            if (scope != RefreshScope.ALL) {
                reserveTriggerInterval(scope);
            }
            return execute(scope, mode);
        } finally {
            redisLock.release(StockMonitorConstants.REFRESH_LOCK, token);
        }
    }

    private void reserveTriggerInterval(RefreshScope scope) {
        String key = scope == RefreshScope.DICTIONARY ? StockMonitorConstants.DICTIONARY_INTERVAL_KEY : StockMonitorConstants.PROFILES_INTERVAL_KEY;
        Duration interval = scope == RefreshScope.DICTIONARY ? Duration.ofMinutes(10) : Duration.ofMinutes(30);
        // 进入外部数据源之前占用间隔，失败后的频繁重试也不会触发数据源风控。
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", interval))) {
            throw new ServiceException(GlobalErrorCode.TOO_MANY_REQUESTS.getCode(), "手动刷新过于频繁，请稍后重试");
        }
    }

    private StockMonitorDtos.RefreshStatus execute(RefreshScope scope, CollectionMode mode) {
        String startedAt = now();
        try {
            if (mode == CollectionMode.AUTO) {
                writeStatus(new StockMonitorDtos.RefreshStatus(true, "RUNNING", startedAt, null, null));
            }
            if (scope != RefreshScope.PROFILES) {
                var dictionary = pythonClient.exchangeDictionary(mode);
                withConfigLock(() -> {
                    // 源请求完成后才取短写锁；字典批次提交成功再重建缓存。
                    transactions.executeWithoutResult(status -> synchronizeDictionary(dictionary));
                    monitorService.rebuildEnabledCache();
                });
            }
            if (scope == RefreshScope.PROFILES || (scope == RefreshScope.ALL && xqEnabled)) {
                refreshEnabledProfiles(mode);
            }
            StockMonitorDtos.RefreshStatus result = new StockMonitorDtos.RefreshStatus(
                    true, "SUCCESS", startedAt, now(), scope == RefreshScope.ALL ? null
                    : scope == RefreshScope.DICTIONARY ? "交易所字典刷新完成" : "已启用股票资料刷新完成");
            if (mode == CollectionMode.AUTO) writeStatus(result);
            return result;
        } catch (Exception exception) {
            LOG.error("个股监控刷新失败", exception);
            StockMonitorDtos.RefreshStatus result = new StockMonitorDtos.RefreshStatus(
                    true, "ERROR", startedAt, now(), "刷新失败，请检查服务日志");
            if (mode == CollectionMode.AUTO) writeStatus(result);
            return result;
        }
    }

    private void refreshEnabledProfiles(CollectionMode mode) {
        List<String> symbols = monitorService.enabledSymbols();
        if (symbols.size() > MarketDataConstants.MAX_MONITORS) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "启用股票超过 10 只");
        }
        if (!symbols.isEmpty()) {
            List<StockMonitorPythonClient.ProfileItem> profiles = pythonClient.profiles(symbols, mode);
            withConfigLock(() -> {
                transactions.executeWithoutResult(status -> synchronizeProfiles(symbols, profiles));
                monitorService.publishResync();
            });
        }
    }

    private void withConfigLock(Runnable action) {
        String token = redisLock.acquire(StockMonitorConstants.CONFIG_LOCK, Duration.ofSeconds(30));
        if (token == null) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "监控清单正在修改，请稍后重试");
        }
        try {
            action.run();
        } finally {
            redisLock.release(StockMonitorConstants.CONFIG_LOCK, token);
        }
    }

    public StockMonitorDtos.RefreshStatus status() {
        String raw = redisTemplate.opsForValue().get(StockMonitorConstants.STATUS_KEY);
        if (raw == null) {
            return new StockMonitorDtos.RefreshStatus(false, "IDLE", null, null, null);
        }
        try {
            com.fasterxml.jackson.databind.JsonNode value = objectMapper.readTree(raw);
            if (!value.isObject() || !value.path("status").isTextual()) {
                throw new IllegalArgumentException("状态格式错误");
            }
            return new StockMonitorDtos.RefreshStatus(value.path("accepted").asBoolean(false),
                    value.path("status").asText(), value.path("startedAt").textValue(),
                    value.path("finishedAt").textValue(), value.path("message").textValue());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "刷新任务状态格式错误");
        }
    }

    private void synchronizeDictionary(List<StockMonitorDtos.DictionaryItem> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "交易所字典为空");
        }
        Set<String> seen = new HashSet<>();
        List<StockSymbolDictionary> validated = new ArrayList<>(stocks.size());
        for (StockMonitorDtos.DictionaryItem item : stocks) {
            if (item == null || item.symbol() == null || !item.symbol().matches("^(SH|SZ|BJ)[0-9]{6}$")
                    || !item.symbol().substring(2).equals(item.code())
                    || !item.symbol().substring(0, 2).equals(item.market())
                    || item.name() == null || item.name().isBlank() || !seen.add(item.symbol())) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "交易所字典数据不合法");
            }
            StockSymbolDictionary entity = new StockSymbolDictionary();
            entity.setSymbol(item.symbol());
            entity.setCode(item.code());
            entity.setName(item.name());
            entity.setMarket(item.market());
            validated.add(entity);
        }
        for (int start = 0; start < validated.size(); start += 300) {
            dictionaryMapper.upsertBatch(validated.subList(start, Math.min(start + 300, validated.size())));
        }
    }

    private void synchronizeProfiles(List<String> symbols, List<StockMonitorPythonClient.ProfileItem> profiles) {
        Set<String> requested = Set.copyOf(symbols);
        Set<String> seen = new HashSet<>();
        for (StockMonitorPythonClient.ProfileItem item : profiles) {
            if (item == null || !requested.contains(item.symbol()) || !seen.add(item.symbol())
                    || !validTime(item.updatedAt()) || !validDate(item.listingDate())) {
                throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "雪球资料数据不合法");
            }
        }
        if (!seen.equals(requested)) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "雪球资料响应缺少选中股票");
        }
        for (StockMonitorPythonClient.ProfileItem item : profiles) {
            StockMonitorConfig config = configMapper.selectOne(StockMonitorConfig::getSymbol, item.symbol());
            if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
                continue;
            }
            StockMonitorProfile entity = profileMapper.selectOne(StockMonitorProfile::getSymbol, item.symbol());
            if (entity == null) {
                entity = new StockMonitorProfile();
                entity.setSymbol(item.symbol());
            }
            entity.setIndustry(item.industry());
            entity.setListingDate(item.listingDate());
            entity.setMarketCap(item.marketCap());
            entity.setUpdatedAt(item.updatedAt());
            if (entity.getId() == null) {
                profileMapper.insert(entity);
            } else {
                profileMapper.updateById(entity);
            }
        }
    }

    private boolean validTime(String time) {
        try {
            return time != null && OffsetDateTime.parse(time).getOffset().getTotalSeconds() == 8 * 3600;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean validDate(String date) {
        try {
            return date == null || LocalDate.parse(date).toString().equals(date);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void writeStatus(StockMonitorDtos.RefreshStatus status) {
        try {
            redisTemplate.opsForValue().set(StockMonitorConstants.STATUS_KEY, objectMapper.writeValueAsString(status));
        } catch (JsonProcessingException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "刷新任务状态写入失败");
        }
    }

    private String now() {
        return OffsetDateTime.now(MarketDataConstants.SHANGHAI).toString();
    }
}
