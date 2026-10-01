package com.vita.controller.pythonjobs;

import com.vita.config.PythonJobsLocalAuthConfiguration;
import com.vita.controller.stockmonitor.StockMonitorRefreshController;
import com.vita.core.exception.ServiceException;
import com.vita.pythonjobs.dto.PythonRunResult;
import com.vita.pythonjobs.service.PythonJobsService;
import com.vita.pythonjobs.service.PythonJobsService.JobKind;
import com.vita.stockmonitor.property.StockMonitorProperty;
import com.vita.stockmonitor.service.StockMonitorRefreshService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PythonJobsLocalControllerTest {
    private final PythonJobsService jobs = mock(PythonJobsService.class);
    private final PythonJobsLocalController controller = new PythonJobsLocalController(jobs);

    @Test
    void threePostRoutesReturnTerminalResultWithoutLoginOrCallerToken() {
        PythonRunResult result = new PythonRunResult("calendar", "SUCCEEDED", "refreshed",
                "2026-10-01T10:00:00+08:00", "2026-10-01T10:00:10+08:00", "done");
        when(jobs.refresh(JobKind.CALENDAR)).thenReturn(result);
        MockHttpServletRequest request = loopback("127.0.0.1");
        assertThat(controller.refreshCalendar(request).getContent()).isSameAs(result);
        verify(jobs).refresh(JobKind.CALENDAR);

        when(jobs.refresh(JobKind.MARKET)).thenReturn(new PythonRunResult("market", "PARTIAL", "partial",
                result.startedAt(), result.finishedAt(), "partial"));
        assertThat(controller.refreshMarket(loopback("::1")).getContent().state()).isEqualTo("PARTIAL");
        when(jobs.refresh(JobKind.MONITOR)).thenReturn(new PythonRunResult("monitor", "SKIPPED", "disabled",
                result.startedAt(), result.finishedAt(), "disabled"));
        assertThat(controller.refreshMonitor(loopback("0:0:0:0:0:0:0:1")).getContent().outcome())
                .isEqualTo("disabled");
    }

    @Test
    void nonLoopbackAndForwardedRequestsAreForbiddenBeforePythonCall() {
        assertForbidden(loopback("10.0.0.5"));
        for (String header : List.of("Forwarded", "X-Forwarded-For", "X-Real-IP")) {
            MockHttpServletRequest request = loopback("127.0.0.1");
            request.addHeader(header, "for=127.0.0.1");
            assertForbidden(request);
        }
        verify(jobs, never()).refresh(JobKind.MARKET);
    }

    @Test
    void onlyThreeExactPostPathsAreAnonymousAndOldTokenRouteStillChecksToken() throws Exception {
        List<String> paths = new PythonJobsLocalAuthConfiguration().pythonJobsLocalPaths().paths();
        assertThat(paths).containsExactly(
                "/local/python-jobs/v1/calendar/refresh",
                "/local/python-jobs/v1/market/refresh",
                "/local/python-jobs/v1/monitor/refresh");
        assertThat(paths).noneMatch(path -> path.contains("/status") || path.contains("*"));
        assertMapping("refreshCalendar", "/calendar/refresh");
        assertMapping("refreshMarket", "/market/refresh");
        assertMapping("refreshMonitor", "/monitor/refresh");

        StockMonitorProperty property = new StockMonitorProperty();
        property.setInternalToken("existing-token");
        StockMonitorRefreshController existing = new StockMonitorRefreshController(
                mock(StockMonitorRefreshService.class), property);
        assertThatThrownBy(() -> existing.refresh(null)).isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(401);
    }

    private void assertMapping(String methodName, String path) throws Exception {
        Method method = PythonJobsLocalController.class.getMethod(methodName, jakarta.servlet.http.HttpServletRequest.class);
        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly(path);
    }

    private void assertForbidden(MockHttpServletRequest request) {
        assertThatThrownBy(() -> controller.refreshMarket(request)).isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(403);
    }

    private MockHttpServletRequest loopback(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
