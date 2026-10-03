package com.vita.etfmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.stockmonitor.property.StockMonitorProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/** 受保护的 Python ETF 字典与资料同步客户端。 */
@Component
public class EtfMonitorPythonClient {
    private final RestClient client;
    private final String baseUrl;
    private final String token;

    public EtfMonitorPythonClient(StockMonitorProperty property) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder().requestFactory(factory).build();
        this.baseUrl = property.getPythonBaseUrl();
        this.token = property.getInternalToken();
    }

    public JsonNode dictionary() {
        return post("/internal/etf-monitor/v1/dictionary", Map.of());
    }

    public JsonNode profiles(Object request) {
        return post("/internal/etf-monitor/v1/profiles", request);
    }

    public JsonNode assetAllocation(Object request) {
        return post("/internal/etf-monitor/v1/asset-allocation", request);
    }

    private JsonNode post(String path, Object body) {
        if (baseUrl == null || baseUrl.isBlank() || token == null || token.isBlank()) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "Python 内部接口未配置");
        }
        try {
            return client.post().uri(baseUrl.replaceAll("/+$", "") + path)
                    .header("X-Internal-Token", token).body(body).retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "Python ETF 同步失败");
        }
    }
}
