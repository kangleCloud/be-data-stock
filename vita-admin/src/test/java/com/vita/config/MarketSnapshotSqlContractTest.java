package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSnapshotSqlContractTest {

    @Test
    void initAndUpgradeExposeOnlySnapshotDashboard() throws IOException {
        String init = read("sql/init/insert.sql");
        String upgrade = read("sql/upgrade/20260923_market_dashboard_snapshot.sql");

        for (String sql : new String[]{init, upgrade}) {
            assertThat(sql).contains("'Market', '/market'", "'MarketOverview', '/market/overview'",
                    "'market/overview/index'", "'market:dashboard:view'",
                    "'/market/dashboard/snapshot'");
            assertThat(sql).doesNotContain("'MarketStockMonitor'", "'market:stock:manage'",
                    "'market:stock:sync'");
        }
        assertThat(upgrade).contains("WHERE @market_menu_id IS NULL",
                "WHERE @market_overview_id IS NULL", "WHERE @market_permission_id IS NULL",
                "NOT EXISTS (SELECT 1 FROM `sys_role_menu`",
                "NOT EXISTS (SELECT 1 FROM `sys_role_permission`");
    }

    private String read(String relativePath) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库文件: " + relativePath);
    }
}
