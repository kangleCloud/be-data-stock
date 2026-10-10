package com.vita.marketdata.etfmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.property.StockMonitorProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
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
        return dictionary(CollectionMode.AUTO);
    }

    public JsonNode dictionary(CollectionMode mode) {
        return post(dictionaryClient, "/internal/etf-monitor/v1/dictionary", Map.of(), mode);
    }

    public JsonNode profiles(Object request) {
        return profiles(request, CollectionMode.AUTO);
    }

    public JsonNode profiles(Object request, CollectionMode mode) {
        return post(profilesClient, "/internal/etf-monitor/v1/profiles", request, mode);
    }

    public JsonNode assetAllocation(Object request) {
        return post(allocationClient, "/internal/etf-monitor/v1/asset-allocation", request, CollectionMode.AUTO);
    }

    private JsonNode post(RestClient client, String path, Object body, CollectionMode mode) {
        if (baseUrl == null || baseUrl.isBlank() || token == null || token.isBlank()) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "Python 内部接口未配置");
        }
        try {
            return client.post().uri(baseUrl.replaceAll("/+$", "") + path)
                    .header("X-Internal-Token", token).header("X-Collection-Mode", mode.getHeaderValue())
                    .body(body).retrieve()
                    .onStatus(code -> code.value() == 409, (request, response) -> {
                        throw new ServiceException(GlobalErrorCode.LOCKED.getCode(), "采集任务正在执行，请等待完成后重试");
                    })
                    .onStatus(code -> code.value() == 429, (request, response) -> {
                        throw new ServiceException(GlobalErrorCode.TOO_MANY_REQUESTS.getCode(), "ETF 同步触发限频或冷却，请稍后重试");
                    })
                    .onStatus(code -> code.isError(), (request, response) -> {
                        throw failure(response.getStatusCode().value(), response.getBody());
                    }).body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "Python ETF 同步失败");
        }
    }

    private ServiceException failure(int status, InputStream body) {
        if (status == 422 || status == 400) {
            return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "ETF 代码或请求报告期不合法");
        }
        String reason = "";
        try {
            // 失败正文只限量解析冻结的分类枚举，不透传 message、令牌或第三方响应。
            JsonNode error = new ObjectMapper().readTree(body.readNBytes(4096));
            if (error != null) reason = error.path("detail").path("reason").asText("");
        } catch (IOException exception) {
            // 旧字符串 detail 或非法正文使用固定通用诊断。
        }
        String message = switch (reason) {
            case "RESOURCE" -> "采集服务资源不足，本次同步未完成，已有报告保留";
            case "NO_DATA" -> "数据源没有该请求报告期的有效资产配置，已有报告保留";
            case "DISABLED" -> "雪球采集总闸或授权已关闭，无法同步资产配置";
            case "SOURCE" -> "资产配置数据源暂不可用，已有报告保留";
            default -> "Python ETF 同步失败，已有资料保留";
        };
        return new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), message);
    }
}
