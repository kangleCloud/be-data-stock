package com.vita.marketdata.service;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.marketdata.dto.PythonRunResult;
import com.vita.marketdata.enums.CollectionMode;
import com.vita.marketdata.enums.PythonJobKind;
import com.vita.marketdata.property.StockMonitorProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Scheduler 直连 Python 并等待手动任务终态；不在 Java 侧创建第二套任务 ID。
 */
@Service
public class PythonJobsService {
    private static final Set<String> TERMINAL_STATES = Set.of("SUCCEEDED", "PARTIAL", "SKIPPED", "FAILED");

    private final Map<PythonJobKind, RestClient> clients;
    private final String baseUrl;
    private final String token;

    @Autowired
    public PythonJobsService(StockMonitorProperty property) {
        this(property, createClients());
    }

    PythonJobsService(StockMonitorProperty property, Map<PythonJobKind, RestClient> clients) {
        this.clients = clients;
        this.baseUrl = property.getPythonBaseUrl();
        this.token = property.getInternalToken();
    }

    public PythonRunResult refresh(PythonJobKind kind) {
        return refresh(kind, CollectionMode.AUTO);
    }

    public PythonRunResult refresh(PythonJobKind kind, CollectionMode mode) {
        String path = "/internal/jobs/v1/" + kind.getPath() + "/refresh";
        try {
            String target = url(path);
            ResponseEntity<PythonRunResult> response = clients.get(kind).post().uri(target)
                    .header("X-Internal-Token", token).header("X-Collection-Mode", mode.getHeaderValue())
                    .body(Map.of()).retrieve()
                    .onStatus(code -> code.value() == 409,
                            (request, result) -> {
                                throw new ServiceException(GlobalErrorCode.LOCKED);
                            })
                    .toEntity(PythonRunResult.class);
            if (response.getStatusCode().value() != 200 || !validResult(response.getBody(), kind)) {
                throw unavailable("Python 任务响应格式错误");
            }
            return response.getBody();
        } catch (ServiceException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (causedBy(exception, SocketTimeoutException.class)) {
                throw new ServiceException(GlobalErrorCode.GATEWAY_TIMEOUT);
            }
            if (causedBy(exception, InterruptedIOException.class)) {
                Thread.currentThread().interrupt();
            }
            throw unavailable("Python 任务服务不可用");
        } catch (RestClientException exception) {
            // 上游鉴权或基础设施错误不透传详情，避免泄露内部响应。
            throw unavailable("Python 任务服务调用失败");
        } catch (IllegalArgumentException exception) {
            throw unavailable("Python 任务服务地址无效");
        }
    }

    private boolean validResult(PythonRunResult result, PythonJobKind kind) {
        return result != null && kind.getPath().equals(result.kind())
                && TERMINAL_STATES.contains(result.state())
                && result.outcome() != null && !result.outcome().isBlank()
                && validTime(result.startedAt()) && validTime(result.finishedAt());
    }

    private boolean validTime(String value) {
        try {
            if (value == null) {
                return false;
            }
            OffsetDateTime.parse(value);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String url(String path) {
        if (baseUrl == null || baseUrl.isBlank() || token == null || token.isBlank()) {
            throw unavailable("Python 内部接口未配置");
        }
        return baseUrl.replaceAll("/+$", "") + path;
    }

    private boolean causedBy(Throwable exception, Class<? extends Throwable> type) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }

    private ServiceException unavailable(String message) {
        return new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), message);
    }

    private static Map<PythonJobKind, RestClient> createClients() {
        Map<PythonJobKind, RestClient> clients = new EnumMap<>(PythonJobKind.class);
        for (PythonJobKind kind : PythonJobKind.values()) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(3));
            requestFactory.setReadTimeout(kind.getReadTimeout());
            clients.put(kind, RestClient.builder().requestFactory(requestFactory).build());
        }
        return clients;
    }

}
