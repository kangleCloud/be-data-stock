package com.vita.marketdata.stockmonitor.service;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.property.StockMonitorProperty;
import com.vita.marketdata.stockmonitor.dto.StockMonitorDtos;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** 内网 Python 采集端；调用入口由刷新服务的雪球开关控制。 */
@Component
public class StockMonitorPythonClient {
    private final RestClient client;
    private final String baseUrl;
    private final String token;

    public StockMonitorPythonClient(StockMonitorProperty property) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
        this.baseUrl = property.getPythonBaseUrl();
        this.token = property.getInternalToken();
    }

    public List<StockMonitorDtos.DictionaryItem> exchangeDictionary() {
        DictionaryResponse response = client.post().uri(url("/internal/stock-monitor/v1/exchange-dictionary"))
                .header("X-Internal-Token", token).body(Map.of()).retrieve().body(DictionaryResponse.class);
        if (response == null || response.schemaVersion() != 1 || response.stocks() == null) {
            throw unavailable("交易所字典响应格式错误");
        }
        return response.stocks();
    }

    public List<ProfileItem> profiles(List<String> symbols) {
        if (symbols.isEmpty()) {
            return List.of();
        }
        ProfileResponse response = client.post().uri(url("/internal/stock-monitor/v1/profiles"))
                .header("X-Internal-Token", token).body(Map.of("symbols", symbols))
                .retrieve().body(ProfileResponse.class);
        if (response == null || response.schemaVersion() != 1 || response.profiles() == null) {
            throw unavailable("雪球资料响应格式错误");
        }
        return response.profiles();
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
