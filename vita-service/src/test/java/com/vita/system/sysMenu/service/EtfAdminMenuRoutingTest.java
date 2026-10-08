package com.vita.system.sysMenu.service;

import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.mapper.SysMenuMapper;
import com.vita.system.sysMenu.service.impl.SysMenuServiceImpl;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class EtfAdminMenuRoutingTest {
    @Test
    void superAdminReceivesAllFourPagesUnderActualSystemParent() {
        SysMenuMapper mapper = mock(SysMenuMapper.class);
        ISysUserService users = mock(ISysUserService.class);
        ISysUserRoleService userRoles = mock(ISysUserRoleService.class);
        ISysRoleMenuService roleMenus = mock(ISysRoleMenuService.class);
        SysMenuServiceImpl service = new SysMenuServiceImpl();
        ReflectionTestUtils.setField(service, "sysMenuMapper", mapper);
        ReflectionTestUtils.setField(service, "sysUserService", users);
        ReflectionTestUtils.setField(service, "sysUserRoleService", userRoles);
        ReflectionTestUtils.setField(service, "sysRoleMenuService", roleMenus);
        SysUser user = new SysUser();
        user.setIsSuperAdmin((byte) 1);
        when(users.getActiveById(1L)).thenReturn(user);
        List<SysMenu> menus = new ArrayList<>();
        menus.add(menu(9000L, 0L, "System", "/system", "Layout", 10));
        String[] names = {"EtfMonitor", "EtfDictionary", "EtfProfile", "MarketIndexConfig"};
        String[] paths = {"etfMonitor", "etfDictionary", "etfProfile", "indexConfig"};
        for (int i = 0; i < names.length; i++) {
            menus.add(menu(9100L + i, 9000L, names[i], "/system/" + paths[i],
                    "system/" + paths[i] + "/index", 17 + i));
        }
        when(mapper.selectAllEnabledMenus()).thenReturn(menus);
        var routes = service.getRoutersByUserId(1L);
        assertEquals(1, routes.size());
        assertEquals("System", routes.get(0).getRouteName());
        assertEquals(List.of(names), routes.get(0).getChildren().stream().map(r -> r.getRouteName()).toList());
        routes.get(0).getChildren().forEach(r -> assertFalse(r.isHidden()));
        verifyNoInteractions(userRoles, roleMenus);
    }

    private SysMenu menu(Long id, Long parent, String name, String path, String component, int sort) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parent);
        menu.setRouteName(name);
        menu.setRouteLink(path);
        menu.setComponentPath(component);
        menu.setVisible((byte) 1);
        menu.setSortNo(sort);
        return menu;
    }
}
