package com.vita.web.xss;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.web.xss.property.XssProperty;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;
import org.springframework.web.filter.FormContentFilter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class XssFilterTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final XssProperty property = new XssProperty();

    @ParameterizedTest
    @ValueSource(strings = {"GET", "DELETE", "POST", "PUT", "PATCH"})
    void queryOrParameterHtmlFailsBeforeBusinessForEveryMethod(String method) throws Exception {
        var request = request(method, "/normal");
        request.addParameter("q", "<script>alert(1)</script>");
        assertRejected(request, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"<script>x</script>", "<img src=x onerror=alert(1)>", "<svg onload=alert(1)></svg>"})
    void nestedJsonAndDuplicateFieldsAreAllChecked(String html) throws Exception {
        var nested = request("POST", "/normal");
        nested.setContentType("application/problem+json");
        nested.setContent(mapper.writeValueAsBytes(java.util.Map.of("array", List.of(java.util.Map.of("name", html)))));
        assertRejected(nested, 400);
        var duplicate = request("POST", "/normal");
        duplicate.setContentType("application/json");
        duplicate.setContent(("{\"name\":" + mapper.writeValueAsString(html) + ",\"name\":\"safe\"}").getBytes(StandardCharsets.UTF_8));
        assertRejected(duplicate, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"中文", "  两边空格  ", "\"引号\"", "&amp;", "收益 < 5 && 6 > 2", "https://example.com/a?x=1&y=2#part", "收益<5>"})
    void validTextParametersAndJsonBytesStayExactlyUnchanged(String text) throws Exception {
        var request = request("POST", "/normal");
        request.setCharacterEncoding("UTF-8");
        request.setContentType("application/json;charset=UTF-8");
        request.addParameter("q", text);
        request.addHeader("Authorization", "<script>header is not input</script>");
        byte[] original = ("  {\"value\": " + mapper.writeValueAsString(text) + "}  ").getBytes(StandardCharsets.UTF_8);
        request.setContent(original);
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();
        filter().doFilter(request, response, (req, res) -> {
            called.set(true);
            var http = (HttpServletRequest) req;
            assertEquals(text, http.getParameter("q"));
            assertArrayEquals(new String[]{text}, http.getParameterValues("q"));
            assertEquals(text, http.getParameterMap().get("q")[0]);
            assertArrayEquals(original, http.getInputStream().readAllBytes());
            var again = http.getInputStream();
            assertFalse(again.isFinished());
            assertArrayEquals(original, again.readAllBytes());
            assertTrue(again.isFinished());
            assertEquals(new String(original, StandardCharsets.UTF_8), http.getReader().readLine());
            assertEquals(request.getHeader("Authorization"), http.getHeader("Authorization"));
        });
        assertTrue(called.get());
        assertEquals(200, response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/api/auth/login", "/app/api/auth/login", "/app/api/auth/register",
            "/admin/api/system/sysUser/add", "/admin/api/system/sysUser/update"})
    void onlyKnownCredentialPostRootPasswordIsExempt(String path) throws Exception {
        String context = path.startsWith("/admin") ? "/admin/api" : "/app/api";
        var request = request("POST", path.substring(context.length()));
        request.setContextPath(context);
        request.setRequestURI(path);
        request.setContentType("application/json");
        request.setContent("{\"password\":\"<script>secret</script>\",\"userName\":\"正常\"}".getBytes(StandardCharsets.UTF_8));
        var called = new AtomicBoolean();
        filter().doFilter(request, new MockHttpServletResponse(), (req,res) -> called.set(true));
        assertTrue(called.get());
        request.setContent("{\"password\":\"secret\",\"nested\":{\"password\":\"<script>x</script>\"}}".getBytes(StandardCharsets.UTF_8));
        assertRejected(request, 400);
        request.setContent("{\"password\":\"secret\",\"userName\":\"<svg/>\"}".getBytes(StandardCharsets.UTF_8));
        assertRejected(request, 400);
    }

    @Test
    void arbitraryPasswordQueryAndOtherMethodsAreNotExempt() throws Exception {
        var request = request("POST", "/normal");
        request.setContentType("application/json");
        request.setContent("{\"password\":\"<script>x</script>\"}".getBytes(StandardCharsets.UTF_8));
        assertRejected(request, 400);
        var query = request("GET", "/auth/login");
        query.addParameter("password", "<svg/>");
        assertRejected(query, 400);
    }

    @Test
    void multipartTextIsCheckedButFileBinaryNeverRead() throws Exception {
        var safe = request("POST", "/upload");
        safe.setContentType("multipart/form-data;boundary=test");
        safe.addPart(new MockPart("file", "test.bin", "<script>binary</script>".getBytes(StandardCharsets.UTF_8)) {
            @Override public java.io.InputStream getInputStream() { throw new AssertionError("二进制不可读取"); }
        });
        safe.addPart(new MockPart("name", "中文 < 5".getBytes(StandardCharsets.UTF_8)));
        var called = new AtomicBoolean();
        filter().doFilter(safe, new MockHttpServletResponse(), (req,res) -> called.set(true));
        assertTrue(called.get());
        var bad = request("POST", "/upload");
        bad.setContentType("multipart/form-data;boundary=test");
        bad.addPart(new MockPart("name", "<svg onload=x/>".getBytes(StandardCharsets.UTF_8)));
        assertRejected(bad, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void formContentFilterMakesNonPostFormVisible(String method) throws Exception {
        var request = request(method, "/normal");
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent("name=%3Csvg+onload%3Dx%3E".getBytes(StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        new FormContentFilter().doFilter(request, response, (req,res) ->
                filter().doFilter(req,res,(unused1,unused2) -> fail("危险表单不能执行业务")));
        assertEquals(400, response.getStatus());
    }

    @Test
    void emptyMalformedAndBoundedJson() throws Exception {
        var empty = request("POST", "/normal"); empty.setContentType("application/json"); empty.setContent(new byte[0]);
        filter().doFilter(empty, new MockHttpServletResponse(), (req,res) -> {
            assertEquals(-1, req.getInputStream().read());
            assertEquals(-1, req.getReader().read());
        });
        var bad = request("POST", "/normal"); bad.setContentType("application/json"); bad.setContent("{bad".getBytes());
        assertRejected(bad, 400);
        property.setMaxJsonBodySize(org.springframework.util.unit.DataSize.ofBytes(4));
        var large = request("POST", "/normal"); large.setContentType("application/json"); large.setContent("{\"x\":1}".getBytes());
        assertRejected(large, 413);
    }

    @Test
    void optionsAsyncErrorDisabledAndExactContextRelativeExclusionSkipOnlyTheirScope() throws Exception {
        for (DispatcherType dispatch : List.of(DispatcherType.ASYNC, DispatcherType.ERROR)) {
            var request = request("GET", "/normal"); request.setDispatcherType(dispatch); request.addParameter("q","<svg/>");
            filter().doFilter(request,new MockHttpServletResponse(),(req,res) -> assertSame(request,req));
        }
        var option = request("OPTIONS", "/normal"); option.addParameter("q","<svg/>");
        filter().doFilter(option,new MockHttpServletResponse(),(req,res) -> assertSame(option,req));
        property.setExcludePaths(List.of("/excluded"));
        var excluded = request("GET", "/excluded"); excluded.addParameter("q","<svg/>");
        filter().doFilter(excluded,new MockHttpServletResponse(),(req,res) -> assertSame(excluded,req));
        var similar = request("GET", "/excluded/child"); similar.addParameter("q","<svg/>"); assertRejected(similar,400);
        property.setEnabled(false); filter().doFilter(similar,new MockHttpServletResponse(),(req,res) -> assertSame(similar,req));
    }

    private XssFilter filter() { return new XssFilter(property, mapper); }

    private MockHttpServletRequest request(String method, String path) {
        var request = new MockHttpServletRequest(method, "/openapi/api" + path);
        request.setContextPath("/openapi/api");
        return request;
    }

    private void assertRejected(MockHttpServletRequest request, int status) throws Exception {
        var response = new MockHttpServletResponse();
        filter().doFilter(request,response,(req,res) -> fail("拒绝请求不能执行业务"));
        assertEquals(status,response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals(status, mapper.readTree(response.getContentAsByteArray()).path("code").asInt());
        assertNull(response.getErrorMessage(), "不用 sendError");
        assertFalse(response.getContentAsString().contains("secret"));
    }
}
