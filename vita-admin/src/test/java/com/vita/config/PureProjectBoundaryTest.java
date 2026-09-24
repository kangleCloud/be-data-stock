package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-08-07
 * @Description: 纯净项目业务模块和数据库边界测试
 * @Version: 1.0
 */
class PureProjectBoundaryTest {

    private static final List<String> REMOVED_RUNTIME_MARKERS = List.of(
            "com.vita.hot",
            "vita.hot",
            "hotRefreshTaskExecutor",
            "marketRefreshTaskExecutor",
            "marketSourceTaskExecutor",
            "market_stock"
    );

    /**
     * 已下线业务包和接口目录不应继续存在。
     */
    @Test
    void removedBusinessModulesShouldNotExist() {
        Path repository = repositoryRoot();

        assertThat(repository.resolve("vita-service/src/main/java/com/vita/hot")).doesNotExist();
        assertThat(repository.resolve("vita-admin/src/main/java/com/vita/controller/hot")).doesNotExist();
        assertThat(repository.resolve(
                "vita-openapi/src/main/java/com/vita/controller/publicapi/HotPublicController.java"))
                .doesNotExist();
        assertThat(repository.resolve(
                "vita-scheduler/src/main/java/com/vita/controller/publicapi/HotRefreshPublicController.java"))
                .doesNotExist();
        assertThat(repository.resolve("vita-scheduler/src/main/java/com/vita/scheduler/market")).doesNotExist();
        assertThat(repository.resolve("sql/init/market.sql")).doesNotExist();
        assertThat(repository.resolve("sql/upgrade/market_upgrade.sql")).doesNotExist();
        assertThat(repository.resolve("sql/upgrade/20260512_hot_cache_permission_upgrade.sql")).doesNotExist();
    }

    /**
     * 主代码和运行配置不得保留已下线模块的类型、配置键或线程池。
     *
     * @throws IOException 文件读取异常
     */
    @Test
    void runtimeSourcesShouldNotReferenceRemovedModules() throws IOException {
        Path repository = repositoryRoot();
        for (Path root : runtimeRoots(repository)) {
            try (var paths = Files.walk(root)) {
                for (Path file : paths.filter(Files::isRegularFile).toList()) {
                    String content = Files.readString(file);
                    assertThat(content)
                            .as(repository.relativize(file).toString())
                            .doesNotContain(REMOVED_RUNTIME_MARKERS.toArray(String[]::new));
                }
            }
        }
    }

    /**
     * 初始化数据不再创建已下线菜单和权限。
     *
     * @throws IOException 文件读取异常
     */
    @Test
    void initSqlShouldNotCreateRemovedBusinessData() throws IOException {
        String initSql = Files.readString(repositoryRoot().resolve("sql/init/insert.sql"));

        assertThat(initSql).doesNotContain(
                "system:hot-cache:view",
                "system:hot-cache:refresh",
                "market:stock:manage",
                "market:stock:sync",
                "'MarketStockMonitor'"
        );
    }

    /**
     * 行情定时任务移除后 scheduler 不再启用定时调度。
     *
     * @throws IOException 文件读取异常
     */
    @Test
    void schedulerShouldNotEnableRemovedCollectionTasks() throws IOException {
        String application = Files.readString(repositoryRoot().resolve(
                "vita-scheduler/src/main/java/com/vita/config/VitaSchedulerApplication.java"));

        assertThat(application).doesNotContain("EnableScheduling", "@Scheduled");
    }

    /**
     * 删除迁移必须覆盖授权、菜单和三张行情表，并保持无 JOIN。
     *
     * @throws IOException 文件读取异常
     */
    @Test
    void cleanupUpgradeShouldRemoveLegacyDataWithoutJoin() throws IOException {
        String sql = Files.readString(repositoryRoot().resolve(
                "sql/upgrade/20260807_remove_hot_market.sql"));
        String upperSql = sql.toUpperCase(Locale.ROOT);

        assertThat(sql).contains(
                "system:hot-cache:view",
                "system:hot-cache:refresh",
                "market:dashboard:view",
                "market:stock:manage",
                "market:stock:sync",
                "DROP TABLE IF EXISTS `market_stock_fund_point`",
                "DROP TABLE IF EXISTS `market_daily_summary`",
                "DROP TABLE IF EXISTS `market_stock`"
        );
        assertThat(upperSql).doesNotContain(" JOIN ");
    }

    /**
     * 返回需要执行残留检查的主代码和配置目录。
     *
     * @param repository 仓库根目录
     * @return 扫描目录
     */
    private List<Path> runtimeRoots(Path repository) {
        return List.of(
                repository.resolve("vita-common/src/main"),
                repository.resolve("vita-service/src/main"),
                repository.resolve("vita-admin/src/main"),
                repository.resolve("vita-openapi/src/main"),
                repository.resolve("vita-scheduler/src/main"),
                repository.resolve("vita-generator/src/main")
        );
    }

    /**
     * 定位 Maven 多模块仓库根目录。
     *
     * @return 仓库根目录
     */
    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("vita-admin"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录");
    }
}
