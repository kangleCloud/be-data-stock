package com.vita.marketdata.stockmonitor.service;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.stockmonitor.dto.StockMonitorDtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** 内网 Python 采集端；调用入口由刷新服务的雪球开关控制。 */
@Component
public class StockMonitorPythonClient {
    private final RestClient client;
    private final String baseUrl;
    private final String token;

    @Autowired
    public StockMonitorPythonClient(StockMonitorProperty property) {
        this(property, createClient());
    }

    StockMonitorPythonClient(StockMonitorProperty property, RestClient client) {
        this.client = client;
        this.baseUrl = property.getPythonBaseUrl();
        this.token = property.getInternalToken();
    }

    private static RestClient createClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    public List<StockMonitorDtos.DictionaryItem> exchangeDictionary() {
        return exchangeDictionary(CollectionMode.AUTO);
    }

    public List<StockMonitorDtos.DictionaryItem> exchangeDictionary(CollectionMode mode) {
        DictionaryResponse response = post("/internal/stock-monitor/v1/exchange-dictionary", Map.of(), mode, DictionaryResponse.class);
        if (response == null || response.schemaVersion() != 1 || response.stocks() == null) {
            throw unavailable("交易所字典响应格式错误");
        }
        return response.stocks();
    }

    public List<ProfileItem> profiles(List<String> symbols) {
        return profiles(symbols, CollectionMode.AUTO);
    }

    public List<ProfileItem> profiles(List<String> symbols, CollectionMode mode) {
        if (symbols.isEmpty()) {
            return List.of();
        }
        ProfileResponse response = post("/internal/stock-monitor/v1/profiles", Map.of("symbols", symbols), mode, ProfileResponse.class);
        if (response == null || response.schemaVersion() != 1 || response.profiles() == null) {
            throw unavailable("雪球资料响应格式错误");
        }
        return response.profiles();
    }

    private <T> T post(String path, Object body, CollectionMode mode, Class<T> responseType) {
        try {
            return client.post().uri(url(path)).header("X-Internal-Token", token)
                    .header("X-Collection-Mode", mode.getHeaderValue()).body(body).retrieve()
                    .onStatus(code -> code.isError(), (request, response) -> {
                        int code = response.getStatusCode().value();
                        if (code == 409) throw new ServiceException(GlobalErrorCode.LOCKED);
                        if (code == 429) throw new ServiceException(GlobalErrorCode.TOO_MANY_REQUESTS);
                        if (code == 400 || code == 422) throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
                        // 不将源正文或鉴权详情带入刷新结果和日志。
                        throw unavailable("Python 股票同步失败");
                    }).body(responseType);
        } catch (ResourceAccessException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException) throw new ServiceException(GlobalErrorCode.GATEWAY_TIMEOUT);
            }
            throw unavailable("Python 股票同步服务不可用");
        } catch (RestClientException exception) {
            throw unavailable("Python 股票同步失败");
        } catch (IllegalArgumentException exception) {
            throw unavailable("Python 股票同步地址无效");
        }
    }

    private String url(String path) {
        if (baseUrl == null || baseUrl.isBlank() || token == null || token.isBlank()) {
            throw unavailable("Python 内部接口未配置");
        }
        return baseUrl.replaceAll("/+$", "") + path;
    }

    private ServiceException unavailable(String message) {
        return new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), message);
    }

    public record ProfileItem(String symbol, String industry, String listingDate,
                              BigDecimal marketCap, String updatedAt) {
    }

    private record DictionaryResponse(int schemaVersion, List<StockMonitorDtos.DictionaryItem> stocks) {
    }

    private record ProfileResponse(int schemaVersion, List<ProfileItem> profiles) {
    }
}
