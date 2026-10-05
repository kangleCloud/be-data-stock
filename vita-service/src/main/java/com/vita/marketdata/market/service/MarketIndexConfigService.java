package com.vita.marketdata.market.service;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.market.dto.MarketIndexConfigDto;
import com.vita.marketdata.market.dto.MarketIndexUpdateRequest;
import com.vita.marketdata.market.entity.MarketIndexConfig;
import com.vita.marketdata.market.enums.CoreIndexEnum;
import com.vita.marketdata.market.mapper.MarketIndexConfigMapper;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/** 原始行情固定采集五只指数，这里只管理公开展示的启停和排序。 */
@Service
public class MarketIndexConfigService {
    private final MarketIndexConfigMapper mapper;
    private final StringRedisTemplate redisTemplate;

    public MarketIndexConfigService(MarketIndexConfigMapper mapper, StringRedisTemplate redisTemplate) {
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
    }

    public List<MarketIndexConfigDto> list() {
        return mapper.selectList(new LambdaQueryWrapperX<MarketIndexConfig>()
                        .orderByAsc(MarketIndexConfig::getSortOrder)
                        .orderByAsc(MarketIndexConfig::getCode))
                .stream().map(this::toDto).toList();
    }

    public List<MarketIndexConfigDto> enabled() {
        return list().stream().filter(MarketIndexConfigDto::enabled).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketIndexConfigDto update(MarketIndexUpdateRequest request) {
        if (request == null || !CoreIndexEnum.codes().contains(request.code()) || request.enabled() == null
                || request.sortOrder() == null || request.sortOrder() < 1 || request.sortOrder() > 5) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "指数配置参数不合法");
        }
        List<MarketIndexConfig> configs = new ArrayList<>(mapper.selectForUpdate());
        if (configs.size() != 5 || !configs.stream().map(MarketIndexConfig::getCode)
                .collect(java.util.stream.Collectors.toSet()).equals(CoreIndexEnum.codes())) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "五指数配置数据不完整");
        }
        MarketIndexConfig config = configs.stream().filter(item -> request.code().equals(item.getCode()))
                .findFirst().orElseThrow(() -> new ServiceException(
                        GlobalErrorCode.NOT_FOUND.getCode(), "指数配置不存在"));
        configs.remove(config);
        configs.add(request.sortOrder() - 1, config);
        config.setEnabled(request.enabled());
        for (int i = 0; i < configs.size(); i++) {
            MarketIndexConfig item = configs.get(i);
            if (item.getSortOrder() == null || item.getSortOrder() != i + 1 || item == config) {
                item.setSortOrder(i + 1);
                mapper.updateById(item);
            }
        }
        TransactionSynchronizationManager.registerSynchronization(new MarketIndexResyncSynchronization(redisTemplate));
        return toDto(config);
    }

    private MarketIndexConfigDto toDto(MarketIndexConfig config) {
        return new MarketIndexConfigDto(config.getCode(), config.getName(),
                Boolean.TRUE.equals(config.getEnabled()), config.getSortOrder());
    }
}
