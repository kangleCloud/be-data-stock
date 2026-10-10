package com.vita.system.sysMenu.service;

import com.vita.core.exception.ServiceException;
import com.vita.system.sysMenu.dto.SysMenuCreateDto;
import com.vita.system.sysMenu.dto.SysMenuUpdateDto;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.mapper.SysMenuMapper;
import com.vita.system.sysMenu.service.impl.SysMenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysMenuExternalLinkTest {
    private SysMenuServiceImpl service;
    private SysMenuMapper mapper;

    @BeforeEach
    void independentMenu() {
        mapper = mock(SysMenuMapper.class);
        service = spy(new SysMenuServiceImpl());
        ReflectionTestUtils.setField(service, "sysMenuMapper", mapper);
        doReturn(true).when(service).checkMenuNameUnique(any());
        doReturn(true).when(service).checkRouteConfigUnique(any());
        var old = new SysMenu();
        old.setId(7L);
        when(mapper.selectById(7L)).thenReturn(old);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/path?x=1&y=2#part", "http://localhost:8080/a",
            "https://[::1]:443/a?q=b#c", "HTTP://Example.COM/中文?q=收益%3C5&x=a%26b#片段"})
    void validHttpUriIsStoredExactlyOnCreateAndUpdate(String link) {
        service.create(create(link, (byte) 1));
        var created = ArgumentCaptor.forClass(SysMenu.class);
        verify(mapper).insert(created.capture());
        assertEquals(link, created.getValue().getRouteLink());
        var update = new SysMenuUpdateDto();
        update.setId(7L);
        update.setIsExternal((byte) 1);
        update.setRouteLink(link);
        service.update(update);
        var updated = ArgumentCaptor.forClass(SysMenu.class);
        verify(mapper).updateById(updated.capture());
        assertEquals(link, updated.getValue().getRouteLink());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:text/html,alert(1)", "file:///tmp/a", "//example.com/a",
            "http://", "https:///a", "http:example.com", "https://bad_.test/a", "https://-bad.test/a",
            "https://example.com:bad/a", "https://example.com:65536/a", "https://example.com/%GG",
            "https://example.com/a b", "https://example.com\\@evil.test/a", "https://example.com/a\n",
            "https://example.com/%09", "https://example.com/?x=%0a", "https://example.com/#%00", ""})
    void unsafeOrMalformedUriFailsBeforeAnyWriteOnBothPaths(String link) {
        var error = assertThrows(ServiceException.class, () -> service.create(create(link, (byte) 1)));
        assertEquals(400, error.getCode());
        var update = new SysMenuUpdateDto();
        update.setId(7L);
        update.setIsExternal((byte) 1);
        update.setRouteLink(link);
        assertEquals(400, assertThrows(ServiceException.class, () -> service.update(update)).getCode());
        verify(mapper, never()).insert(any(SysMenu.class));
        verify(mapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    void internalMenuPathRemainsUnchanged() {
        service.create(create("/system/etfMonitor", (byte) 0));
        var row = ArgumentCaptor.forClass(SysMenu.class);
        verify(mapper).insert(row.capture());
        assertEquals("/system/etfMonitor", row.getValue().getRouteLink());
    }

    @Test
    void menuSortUsesNumericBeanValidationOnCreateAndInheritedUpdate() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var dto = create("https://example.com/a", (byte) 1);
            dto.setMenuType("LINK"); dto.setSortNo(10);
            assertTrue(factory.getValidator().validate(dto).isEmpty());
            dto.setSortNo(1000);
            assertFalse(factory.getValidator().validate(dto).isEmpty());
            var update = new SysMenuUpdateDto(); update.setId(7L); update.setMenuName("菜单");
            update.setMenuType("LINK"); update.setSortNo(-1);
            assertFalse(factory.getValidator().validate(update).isEmpty());
        }
    }

    private SysMenuCreateDto create(String link, byte external) {
        var dto = new SysMenuCreateDto();
        dto.setMenuName("基金 & 收益<5>");
        dto.setIsExternal(external);
        dto.setRouteLink(link);
        return dto;
    }
}
