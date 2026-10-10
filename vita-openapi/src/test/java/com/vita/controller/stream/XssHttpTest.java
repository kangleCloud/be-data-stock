package com.vita.controller.stream;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.log.interceptor.RequestTraceInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes=XssHttpTestConfiguration.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"spring.config.location=optional:classpath:/sse-handshake-test.yml",
                "server.servlet.context-path=/admin/api", "logging.config=classpath:logback-console.xml",
                "spring.main.banner-mode=off", "vita.auth.extra-exclude-paths[0]=/auth/login",
                "spring.servlet.multipart.max-file-size=1KB", "spring.servlet.multipart.max-request-size=2KB"})
class XssHttpTest {
    @LocalServerPort private int port;
    @Autowired private StpLogic login;
    @Autowired private RequestTraceInterceptor trace;
    @Autowired private XssEchoTestController business;
    @Autowired private jakarta.servlet.ServletContext servletContext;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @BeforeEach
    void resetMocks() throws Exception {
        reset(login,trace); business.reset(); when(trace.preHandle(any(),any(),any())).thenReturn(true);
        assertEquals(1,servletContext.getFilterRegistrations().values().stream()
                .filter(filter -> filter.getClassName().equals("com.vita.web.xss.XssFilter")).count());
    }

    @ParameterizedTest
    @ValueSource(strings={"GET","DELETE","POST","PUT","PATCH"})
    void dangerousQueryNeverReachesBusiness(String method) throws Exception {
        var response=send(method,"/xss-test/echo?q="+encode("<svg onload=x/>"),null,null);
        assertError(response,400); assertEquals(0,business.invocations()); verifyNoInteractions(login);
    }

    @ParameterizedTest
    @ValueSource(strings={"POST","PUT","PATCH","DELETE"})
    void dangerousFormNeverReachesBusiness(String method) throws Exception {
        var response=send(method,"/xss-test/echo","application/x-www-form-urlencoded","q="+encode("<img onerror=x>"));
        assertError(response,400); assertEquals(0,business.invocations());
    }

    @Test
    void nestedJsonBlockedAndNormalBodyParametersHeadersPreserved() throws Exception {
        assertError(send("POST","/xss-test/echo","application/json","{\"nested\":[{\"name\":\"<script>x</script>\"}]}"),400);
        assertEquals(0,business.invocations());
        String text="  中文 &amp; 收益 < 5 \"报价\"  ";
        String raw=" {\"name\":"+mapper.writeValueAsString(text)+"} ";
        var response=send("POST","/xss-test/echo?q="+encode(text),"application/json",raw);
        assertEquals(200,response.statusCode());
        JsonNode result=mapper.readTree(response.body());
        assertEquals(raw,result.path("body").asText()); assertEquals(raw,result.path("reader").asText());
        assertEquals(text,result.path("parameters").path("q").get(0).asText());
        assertEquals("<script>header</script>",result.path("token").asText());
    }

    @Test
    void jsonMediaTypeAndInvalidSyntaxHaveNormal400Semantics() throws Exception {
        assertError(send("POST","/xss-test/echo","application/vnd.vita+json","{\"x\":\"<svg/>\"}"),400);
        assertError(send("POST","/xss-test/echo","application/json","{bad"),400);
        assertEquals(0,business.invocations());
    }

    @Test
    void loginRootPasswordPreservedWhileUnsafeUserNameRejected() throws Exception {
        String secret="  <script>password</script> & \"  ";
        var raw=mapper.writeValueAsString(java.util.Map.of("userName","正常用户","password",secret));
        var response=send("POST","/auth/login","application/json",raw);
        assertEquals(200,response.statusCode()); assertEquals(secret,mapper.readTree(response.body()).path("password").asText());
        verifyNoInteractions(login);
        business.reset();
        assertError(send("POST","/auth/login","application/json","{\"userName\":\"<svg/>\",\"password\":\"plain\"}"),400);
        assertEquals(0,business.invocations());
    }

    @Test
    void multipartTextRejectedAndBinaryArrivesIntactWithExistingSizeLimit() throws Exception {
        String file="<script>binary</script>";
        var safe=send("POST","/xss-test/echo","multipart/form-data;boundary=xsstest",multipart("中文 < 5",file));
        assertEquals(200,safe.statusCode()); assertEquals(file,mapper.readTree(safe.body()).path("file").asText());
        business.reset();
        assertError(send("POST","/xss-test/echo","multipart/form-data;boundary=xsstest",multipart("<svg onload=x/>",file)),400);
        assertEquals(0,business.invocations());
        assertError(send("POST","/xss-test/echo","multipart/form-data;boundary=xsstest",multipart("normal","x".repeat(1500))),413);
    }

    @Test
    void optionsAndExistingAuthenticationAndAuthorizationSemanticsRemain() throws Exception {
        var response=send("OPTIONS","/xss-test/echo?q="+encode("<svg/>"),null,null);
        assertEquals(200,response.statusCode(), "OPTIONS 跳过 XSS 检测，沿用现有 MVC／鉴权行为");
        reset(login); business.reset();
        doThrow(new IllegalStateException("测试未登录")).when(login).checkLogin();
        var unauthorized=send("GET","/xss-test/echo",null,null);
        assertEquals(200,unauthorized.statusCode()); assertEquals(401,mapper.readTree(unauthorized.body()).path("code").asInt());
        reset(login);
        var forbidden=send("GET","/xss-test/forbidden",null,null);
        assertEquals(200,forbidden.statusCode()); assertEquals(403,mapper.readTree(forbidden.body()).path("code").asInt());
    }

    private String multipart(String name,String file) {
        return "--xsstest\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n"+name
                +"\r\n--xsstest\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.bin\"\r\n"
                +"Content-Type: application/octet-stream\r\n\r\n"+file+"\r\n--xsstest--\r\n";
    }

    private String encode(String value) {return URLEncoder.encode(value,StandardCharsets.UTF_8);}

    private HttpResponse<String> send(String method,String path,String type,String body) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/admin/api"+path)).timeout(Duration.ofSeconds(3))
                .header("Authorization","<script>header</script>");
        if(type!=null)request.header("Content-Type",type);
        request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8));
        return client.send(request.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertError(HttpResponse<String> response,int status) throws Exception {
        assertEquals(status,response.statusCode()); assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(status,mapper.readTree(response.body()).path("code").asInt());
        assertFalse(response.body().contains("<svg")); assertFalse(response.body().contains("secret"));
    }
}
