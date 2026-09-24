package com.vita.log.service;

import com.vita.core.property.VitaProperty;
import com.vita.redis.RedisCache;
import com.vita.utils.web.ip.AddressUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpLocationServiceTest {

    @Mock
    private RedisCache redisCache;

    @AfterEach
    void tearDown() {
        VitaProperty.setAddressEnabled(false);
    }

    @Test
    void resolveFastShouldReturnUnknownWhenCacheMissed() {
        VitaProperty.setAddressEnabled(true);
        when(redisCache.getCacheObject("ip_location:8.8.8.8")).thenReturn(null);

        IpLocationService ipLocationService = new IpLocationService(redisCache);

        assertEquals(AddressUtils.UNKNOWN, ipLocationService.resolveFast("8.8.8.8"));
        assertTrue(ipLocationService.needsAsyncEnrichment("8.8.8.8", AddressUtils.UNKNOWN));
    }

    @Test
    void resolveFastShouldReturnInternalIpLabel() {
        VitaProperty.setAddressEnabled(true);

        IpLocationService ipLocationService = new IpLocationService(redisCache);

        assertEquals("内网IP", ipLocationService.resolveFast("10.0.0.8"));
    }

    @Test
    void resolveAndCacheShouldStoreLookupResult() {
        VitaProperty.setAddressEnabled(true);
        when(redisCache.getCacheObject("ip_location:8.8.8.8")).thenReturn(null);

        IpLocationService ipLocationService = new IpLocationService(redisCache);

        try (MockedStatic<AddressUtils> addressUtilsMockedStatic = mockStatic(AddressUtils.class, CALLS_REAL_METHODS)) {
            addressUtilsMockedStatic.when(() -> AddressUtils.getRealAddressByIP("8.8.8.8", 500))
                    .thenReturn("California Mountain View");

            assertEquals("California Mountain View", ipLocationService.resolveAndCache("8.8.8.8"));
        }

        verify(redisCache).setCacheObject(eq("ip_location:8.8.8.8"), eq("California Mountain View"), anyLong(), eq(TimeUnit.HOURS));
    }
}
