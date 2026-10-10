package com.vita.controller.stream;

import cn.dev33.satoken.stp.StpLogic;
import com.vita.log.interceptor.RequestTraceInterceptor;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(classes=XssHttpTestConfiguration.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"spring.config.location=optional:classpath:/sse-handshake-test.yml",
                "server.servlet.context-path=/scheduler/api", "logging.config=classpath:logback-console.xml",
                "spring.main.banner-mode=off", "vita.xss.url-patterns[0]=/xss-test/*",
                "vita.xss.exclude-paths[0]=/xss-test/excluded"})
class XssPatternHttpTest {
    @LocalServerPort private int port;
    @Autowired private StpLogic login;
    @Autowired private RequestTraceInterceptor trace;
    @Autowired private ServletContext servletContext;

    @Test
    void onlyMappedRequestsAreCheckedAndExclusionIsExactAfterContextPathRemoval() throws Exception {
        reset(login,trace); when(trace.preHandle(any(),any(),any())).thenReturn(true);
        assertEquals(1,servletContext.getFilterRegistrations().values().stream()
                .filter(filter -> filter.getClassName().equals("com.vita.web.xss.XssFilter")).count());
        assertEquals(200,get("/outside"));
        assertEquals(200,get("/xss-test/excluded"));
        assertEquals(400,get("/xss-test/excluded/child"));
        assertEquals(400,get("/xss-test/echo"));
    }

    private int get(String path) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/scheduler/api"+path+"?q=%3Csvg%2F%3E"))
                .timeout(Duration.ofSeconds(3)).build();
        return HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString()).statusCode();
    }
}
