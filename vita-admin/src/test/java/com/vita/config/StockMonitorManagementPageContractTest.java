package com.vita.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.controller.stockmonitor.StockDictionaryAdminController;
import com.vita.controller.stockmonitor.StockMonitorAdminController;
import com.vita.controller.stockmonitor.StockProfileAdminController;
import com.vita.stockmonitor.dto.StockDictionaryCreateDto;
import com.vita.stockmonitor.dto.StockDictionaryPageQuery;
import com.vita.stockmonitor.dto.StockMonitorPageQuery;
import com.vita.stockmonitor.dto.StockProfilePageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StockMonitorManagementPageContractTest {

    @Test
    void threeManagementPagesHaveSeparateViewPermissions() throws NoSuchMethodException {
        assertPage(StockMonitorAdminController.class, StockMonitorPageQuery.class,
                "/system/stockMonitor", "system:stock-monitor:view");
        assertPage(StockDictionaryAdminController.class, StockDictionaryPageQuery.class,
                "/system/stockDictionary", "system:stock-dictionary:view");
        assertPage(StockProfileAdminController.class, StockProfilePageQuery.class,
                "/system/stockProfile", "system:stock-profile:view");
    }

    @Test
    void manualDictionaryAddHasItsOwnPermission() throws NoSuchMethodException {
        Method method = StockDictionaryAdminController.class.getMethod("add", StockDictionaryCreateDto.class);
        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly("/add");
        assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("system:stock-dictionary:add");
    }

    @Test
    void initializationAndUpgradeGrantOnlySuperAdminByDefault() throws IOException {
        String init = Files.readString(repositoryFile("sql/init/insert.sql"));
        String upgrade = Files.readString(repositoryFile("sql/upgrade/20260928_stock_monitor_v1.sql"));
        assertThat(init).contains("'StockMonitor'", "'StockDictionary'", "'StockProfile'",
                "'system:stock-monitor:view'", "'system:stock-monitor:update'",
                "'system:stock-monitor:refresh'", "'system:stock-dictionary:view'",
                "'system:stock-profile:view'", "'system:stock-dictionary:add'", "(4009, 1, 1004", "(4010, 1, 1005",
                "(4011, 1, 1006", "(5063, 1, 2045", "(5067, 1, 2049", "(5068, 1, 2050");
        assertThat(upgrade).contains("`role_code` = 'SUPER_ADMIN'", "NOT EXISTS",
                "'StockDictionary'", "'StockProfile'", "'system:stock-dictionary:view'",
                "'system:stock-profile:view'", "'system:stock-dictionary:add'");
    }

    private void assertPage(Class<?> controller, Class<?> queryType, String route, String permission)
            throws NoSuchMethodException {
        assertThat(controller.getAnnotation(RequestMapping.class).value()).containsExactly(route);
        Method method = controller.getMethod("page", queryType);
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/page");
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
    }

    private Path repositoryFile(String relativePath) {
        for (Path current = Path.of("").toAbsolutePath(); current != null; current = current.getParent()) {
            Path file = current.resolve(relativePath);
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        throw new IllegalStateException("Repository file not found: " + relativePath);
    }
}
