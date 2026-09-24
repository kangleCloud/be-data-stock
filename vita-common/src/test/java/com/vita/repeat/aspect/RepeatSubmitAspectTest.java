package com.vita.repeat.aspect;

import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import com.vita.core.exception.ServiceException;
import com.vita.redis.RedisCache;
import com.vita.repeat.annotation.RepeatSubmit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepeatSubmitAspectTest {

    @Mock
    private RedisCache redisCache;

    private RepeatSubmitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RepeatSubmitAspect(redisCache);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        LoginUserInfoModelContext.removeLoginUserInfo();
    }

    @Test
    void aroundShouldBlockRepeatedRequestWithinWindow() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/sysUser/add");
        request.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        LoginUserInfoModel loginUserInfoModel = new LoginUserInfoModel();
        loginUserInfoModel.setId(42L);
        LoginUserInfoModelContext.setLoginUserInfo(loginUserInfoModel);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"payload", 1});
        when(joinPoint.proceed()).thenReturn("ok");
        when(redisCache.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true, false);

        RepeatSubmit repeatSubmit = mock(RepeatSubmit.class);
        when(repeatSubmit.interval()).thenReturn(5L);
        when(repeatSubmit.unit()).thenReturn(TimeUnit.SECONDS);

        Object result = aspect.around(joinPoint, repeatSubmit);
        assertEquals("ok", result);

        ServiceException exception = assertThrows(ServiceException.class, () -> aspect.around(joinPoint, repeatSubmit));
        assertEquals(900, exception.getCode());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisCache, times(2)).setIfAbsent(keyCaptor.capture(), any(), anyLong(), any());
        assertTrue(keyCaptor.getAllValues().get(0).startsWith("repeat_submit:POST:/system/sysUser/add:42:"));
        verify(joinPoint).proceed();
    }

    @Test
    void aroundShouldUseIpWhenUserIsAbsent() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/hot/cache/refresh");
        request.setRemoteAddr("10.0.0.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"sourceCode"});
        when(joinPoint.proceed()).thenReturn("ok");
        when(redisCache.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);

        RepeatSubmit repeatSubmit = mock(RepeatSubmit.class);
        when(repeatSubmit.interval()).thenReturn(5L);
        when(repeatSubmit.unit()).thenReturn(TimeUnit.SECONDS);

        aspect.around(joinPoint, repeatSubmit);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisCache).setIfAbsent(keyCaptor.capture(), any(), anyLong(), any());
        assertTrue(keyCaptor.getValue().contains(":10.0.0.9:"));
    }
}
