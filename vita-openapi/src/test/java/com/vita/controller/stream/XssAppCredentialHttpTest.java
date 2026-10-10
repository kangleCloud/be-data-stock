package com.vita.controller.stream;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.log.interceptor.RequestTraceInterceptor;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes=XssHttpTestConfiguration.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"spring.config.location=optional:classpath:/sse-handshake-test.yml",
                "server.servlet.context-path=/app/api", "logging.config=classpath:logback-console.xml",
                "spring.main.banner-mode=off", "vita.auth.extra-exclude-paths[0]=/auth/login",
                "vita.auth.extra-exclude-paths[1]=/auth/register"})
class XssAppCredentialHttpTest {
    @LocalServerPort private int port;
    @Autowired private StpLogic login;
    @Autowired private RequestTraceInterceptor trace;
    @Autowired private XssEchoTestController business;
    @Autowired private ServletContext servletContext;

    @BeforeEach
    void prepare() throws Exception {
        reset(login,trace); business.reset(); when(trace.preHandle(any(),any(),any())).thenReturn(true);
        assertEquals(1,servletContext.getFilterRegistrations().values().stream()
                .filter(filter -> filter.getClassName().equals("com.vita.web.xss.XssFilter")).count());
    }

    @ParameterizedTest
    @ValueSource(strings={"/auth/login","/auth/register"})
    void appRootPasswordIsPreservedButNestedAndOtherFieldsAreRejected(String path) throws Exception {
        String raw="{\"userName\":\"中文\",\"password\":\" <script>secret</script> & \"}";
        var response=post(path,raw);
        assertEquals(200,response.statusCode()); assertEquals(1,business.invocations());
        assertEquals(" <script>secret</script> & ",new ObjectMapper().readTree(response.body()).path("password").asText());
        verifyNoInteractions(login);
        business.reset();
        var nested=post(path,"{\"nested\":{\"password\":\"<svg/>\"}}");
        assertEquals(400,nested.statusCode()); assertEquals(0,business.invocations());
        var other=post("/xss-test/echo",raw); assertEquals(400,other.statusCode()); assertEquals(0,business.invocations());
        assertFalse(other.body().contains("secret"));
    }

    private HttpResponse<String> post(String path,String body) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/app/api"+path))
                .timeout(Duration.ofSeconds(3)).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
    }
}
