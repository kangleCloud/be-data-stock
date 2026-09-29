package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSnapshotSqlContractTest {

    @Test
    void newDatabaseHasNoAdminMarketMenuOrPermission() throws IOException {
        String init = read("sql/init/insert.sql");
        assertThat(init).doesNotContain("MarketOverview", "market:dashboard:view",
                "'/market/dashboard/snapshot'");
    }

    @Test
    void existingDatabaseCleanupTargetsOldPermissionAndMenuRelations() throws IOException {
        String cleanup = read("sql/upgrade/20260925_remove_admin_market_dashboard.sql");
        assertThat(cleanup).contains("'market:dashboard:view'",
                "'MarketOverview'", "'Market'", "`sys_role_permission`",
                "`sys_role_menu`", "@market_root_children = 0");
        assertThat(cleanup).doesNotContain("DROP TABLE", "TRUNCATE");
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
