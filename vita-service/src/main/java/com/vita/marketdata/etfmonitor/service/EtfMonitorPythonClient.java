package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.property.StockMonitorProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/** 受保护的 Python ETF 字典与资料同步客户端。 */
@Component
public class EtfMonitorPythonClient {
    static final Duration DICTIONARY_READ_TIMEOUT = Duration.ofSeconds(60);
    static final Duration PROFILES_READ_TIMEOUT = Duration.ofSeconds(210);
    static final Duration ALLOCATION_READ_TIMEOUT = Duration.ofSeconds(120);
    private final RestClient dictionaryClient;
    private final RestClient profilesClient;
    private final RestClient allocationClient;
    private final String baseUrl;
    private final String token;

    @Autowired
    public EtfMonitorPythonClient(StockMonitorProperty property) {
        this(property, client(DICTIONARY_READ_TIMEOUT), client(PROFILES_READ_TIMEOUT),
                client(ALLOCATION_READ_TIMEOUT));
    }

    EtfMonitorPythonClient(StockMonitorProperty property, RestClient dictionaryClient,
                          RestClient profilesClient, RestClient allocationClient) {
        this.dictionaryClient = dictionaryClient;
        this.profilesClient = profilesClient;
        this.allocationClient = allocationClient;
        this.baseUrl = property.getPythonBaseUrl();
        this.token = property.getInternalToken();
    }

    private static RestClient client(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(factory).build();
    }

    public JsonNode dictionary() {
        return post(dictionaryClient, "/internal/etf-monitor/v1/dictionary", Map.of());
    }

    public JsonNode profiles(Object request) {
        return post(profilesClient, "/internal/etf-monitor/v1/profiles", request);
    }

    public JsonNode assetAllocation(Object request) {
        return post(allocationClient, "/internal/etf-monitor/v1/asset-allocation", request);
    }

    private JsonNode post(RestClient client, String path, Object body) {
        if (baseUrl == null || baseUrl.isBlank() || token == null || token.isBlank()) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "Python 内部接口未配置");
        }
        try {
            return client.post().uri(baseUrl.replaceAll("/+$", "") + path)
                    .header("X-Internal-Token", token).body(body).retrieve()
                    .onStatus(code -> code.value() == 409, (request, response) -> {
                        throw new ServiceException(GlobalErrorCode.LOCKED.getCode(), "ETF 资料批次正在执行");
                    })
                    .onStatus(code -> code.value() == 429, (request, response) -> {
                        throw new ServiceException(GlobalErrorCode.TOO_MANY_REQUESTS.getCode(), "ETF 资料刷新未满足 30 分钟间隔");
                    }).body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "Python ETF 同步失败");
        }
    }
}
