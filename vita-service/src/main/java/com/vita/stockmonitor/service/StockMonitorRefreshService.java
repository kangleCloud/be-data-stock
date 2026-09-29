package com.vita.stockmonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.stockmonitor.dto.StockMonitorDtos;
import com.vita.stockmonitor.entity.StockMonitorConfig;
import com.vita.stockmonitor.entity.StockMonitorProfile;
import com.vita.stockmonitor.entity.StockSymbolDictionary;
import com.vita.stockmonitor.mapper.StockMonitorConfigMapper;
import com.vita.stockmonitor.mapper.StockMonitorProfileMapper;
import com.vita.stockmonitor.mapper.StockSymbolDictionaryMapper;
import com.vita.stockmonitor.property.StockMonitorProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/** 交易所字典与最多 10 只选中股票资料的整体刷新。 */
@Service
public class StockMonitorRefreshService {
    private static final Logger LOG = LoggerFactory.getLogger(StockMonitorRefreshService.class);
    private static final String LOCK_KEY = "stock:monitor:v1:refresh:lock";
    private static final String CONFIG_LOCK = "stock:monitor:v1:config:lock";
    private static final String STATUS_KEY = "stock:monitor:v1:refresh:status";
    private static final String DICTIONARY_INTERVAL_KEY = "stock:monitor:v1:refresh:dictionary:interval";
    private static final String PROFILES_INTERVAL_KEY = "stock:monitor:v1:refresh:profiles:interval";
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final StockMonitorPythonClient pythonClient;
    private final StockSymbolDictionaryMapper dictionaryMapper;
    private final StockMonitorConfigMapper configMapper;
    private final StockMonitorProfileMapper profileMapper;
    private final StockMonitorService monitorService;
    private final StockMonitorRedisLock redisLock;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean xqEnabled;

    private enum RefreshScope { ALL, DICTIONARY, PROFILES }

    public StockMonitorRefreshService(StockMonitorPythonClient pythonClient,
                                      StockSymbolDictionaryMapper dictionaryMapper,
                                      StockMonitorConfigMapper configMapper,
                                      StockMonitorProfileMapper profileMapper,
                                      StockMonitorService monitorService,
                                      StockMonitorRedisLock redisLock,
                                      StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper,
                                      StockMonitorProperty property) {
        this.pythonClient = pythonClient;
        this.dictionaryMapper = dictionaryMapper;
        this.configMapper = configMapper;
        this.profileMapper = profileMapper;
        this.monitorService = monitorService;
        this.redisLock = redisLock;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.xqEnabled = property.isXqEnabled();
    }

    public StockMonitorDtos.RefreshStatus refresh() {
        return refresh(RefreshScope.ALL);
    }

    public StockMonitorDtos.RefreshStatus refreshDictionary() {
        return refresh(RefreshScope.DICTIONARY);
    }

    public StockMonitorDtos.RefreshStatus refreshProfiles() {
        if (!xqEnabled) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "雪球采集总闸已关闭");
        }
        return refresh(RefreshScope.PROFILES);
    }

    private StockMonitorDtos.RefreshStatus refresh(RefreshScope scope) {
        String token = redisLock.acquire(LOCK_KEY, Duration.ofMinutes(15));
        if (token == null) {
            StockMonitorDtos.RefreshStatus current = status();
            return new StockMonitorDtos.RefreshStatus(false, current.jobId(), current.status(),
                    current.startedAt(), current.finishedAt(), "刷新任务正在执行");
        }
        try {
            if (scope != RefreshScope.ALL) {
                reserveManualInterval(scope);
            }
            return executeLocked(scope);
        } finally {
            redisLock.release(LOCK_KEY, token);
        }
    }

    private void reserveManualInterval(RefreshScope scope) {
        String key = scope == RefreshScope.DICTIONARY ? DICTIONARY_INTERVAL_KEY : PROFILES_INTERVAL_KEY;
        Duration interval = scope == RefreshScope.DICTIONARY ? Duration.ofMinutes(10) : Duration.ofMinutes(30);
        // 进入外部数据源之前占用间隔，失败后的频繁重试也不会触发数据源风控。
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", interval))) {
            throw new ServiceException(GlobalErrorCode.TOO_MANY_REQUESTS.getCode(), "手动刷新过于频繁，请稍后重试");
        }
    }

    private StockMonitorDtos.RefreshStatus executeLocked(RefreshScope scope) {
        String jobId = UUID.randomUUID().toString();
        String startedAt = now();
        try {
            writeStatus(new StockMonitorDtos.RefreshStatus(true, jobId, "RUNNING", startedAt, null, null));
            if (scope != RefreshScope.PROFILES) {
                synchronizeDictionary(pythonClient.exchangeDictionary());
                withConfigLock(monitorService::rebuildEnabledCache);
            }
            if (scope == RefreshScope.PROFILES || (scope == RefreshScope.ALL && xqEnabled)) {
                refreshEnabledProfiles();
            }
            StockMonitorDtos.RefreshStatus result = new StockMonitorDtos.RefreshStatus(
                    true, jobId, "SUCCESS", startedAt, now(), scope == RefreshScope.ALL ? null
                    : scope == RefreshScope.DICTIONARY ? "交易所字典刷新完成" : "已启用股票资料刷新完成");
            writeStatus(result);
            return result;
        } catch (Exception exception) {
            LOG.error("个股监控刷新失败", exception);
            StockMonitorDtos.RefreshStatus result = new StockMonitorDtos.RefreshStatus(
                    true, jobId, "ERROR", startedAt, now(), "刷新失败，请检查服务日志");
            writeStatus(result);
            return result;
        }
    }

    private void refreshEnabledProfiles() {
        List<String> symbols = monitorService.enabledSymbols();
        if (symbols.size() > 10) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "启用股票超过 10 只");
        }
        if (!symbols.isEmpty()) {
            List<StockMonitorPythonClient.ProfileItem> profiles = pythonClient.profiles(symbols);
            withConfigLock(() -> synchronizeProfiles(symbols, profiles));
        }
    }

    private void withConfigLock(Runnable action) {
        String token = redisLock.acquire(CONFIG_LOCK, Duration.ofSeconds(30));
        if (token == null) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "监控清单正在修改，请稍后重试");
        }
        try {
            action.run();
        } finally {
            redisLock.release(CONFIG_LOCK, token);
        }
    }

    public StockMonitorDtos.RefreshStatus status() {
        String raw = redisTemplate.opsForValue().get(STATUS_KEY);
        if (raw == null) {
            return new StockMonitorDtos.RefreshStatus(false, null, "IDLE", null, null, null);
        }
        try {
            return objectMapper.readValue(raw, StockMonitorDtos.RefreshStatus.class);
        } catch (JsonProcessingException exception) {
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
            redisTemplate.opsForValue().set(STATUS_KEY, objectMapper.writeValueAsString(status));
        } catch (JsonProcessingException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "刷新任务状态写入失败");
        }
    }

    private String now() {
        return OffsetDateTime.now(SHANGHAI).toString();
    }
}
