package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户认证数据库脚本回归测试
 * @Version: 1.0
 */
class AppAuthSqlSchemaTest {

    private static final String INIT_SQL = "sql/init/app.sql";
    private static final String UPGRADE_SQL = "sql/upgrade/20260825_vita_app_auth_upgrade.sql";
    private static final List<String> APP_USER_COLUMNS = List.of(
            "id", "tenant_id", "user_name", "password", "nick_name", "avatar_url", "status",
            "login_time", "login_address", "pwd_update_date", "create_time", "update_time", "is_deleted",
            "create_by_id", "create_by", "update_by_id", "update_by", "version", "remark"
    );
    private static final List<String> APP_OAUTH_ACCOUNT_COLUMNS = List.of(
            "id", "tenant_id", "user_id", "provider_code", "provider_user_id", "provider_login",
            "provider_nick_name", "avatar_url", "profile_url", "bind_time", "last_auth_time", "create_time",
            "update_time", "is_deleted", "create_by_id", "create_by", "update_by_id", "update_by", "version",
            "remark"
    );

    /**
     * 初始化和升级脚本必须保持相同表结构及字段顺序。
     *
     * @throws IOException 脚本读取异常
     */
    @Test
    void initAndUpgradeScriptsShouldContainSameSchema() throws IOException {
        String initSql = readRepositoryFile(INIT_SQL);
        String upgradeSql = readRepositoryFile(UPGRADE_SQL);

        assertThat(extractColumns(initSql, "app_user")).containsExactlyElementsOf(APP_USER_COLUMNS);
        assertThat(extractColumns(initSql, "app_oauth_account")).containsExactlyElementsOf(APP_OAUTH_ACCOUNT_COLUMNS);
        assertThat(normalizeDdl(extractCreateTable(initSql, "app_user")))
                .isEqualTo(normalizeDdl(extractCreateTable(upgradeSql, "app_user")));
        assertThat(normalizeDdl(extractCreateTable(initSql, "app_oauth_account")))
                .isEqualTo(normalizeDdl(extractCreateTable(upgradeSql, "app_oauth_account")));
    }

    /**
     * 用户名和 OAuth 身份必须使用不可因逻辑删除而回收的全局唯一约束。
     *
     * @throws IOException 脚本读取异常
     */
    @Test
    void schemaShouldEnforceGlobalIdentityUniqueness() throws IOException {
        String sql = readRepositoryFile(INIT_SQL).toLowerCase(Locale.ROOT);

        assertThat(sql).contains(
                "unique key `uk_app_user_name` (`user_name`)",
                "unique key `uk_app_oauth_provider_user` (`provider_code`, `provider_user_id`)",
                "unique key `uk_app_oauth_user_provider` (`user_id`, `provider_code`)"
        );
        assertThat(sql).doesNotContain(
                "key `idx_app_user_name`",
                "(`user_name`, `is_deleted`)",
                "(`provider_code`, `provider_user_id`, `is_deleted`)"
        );
        assertThat(sql).contains(
                "`provider_code`      varchar(32) character set utf8mb4 collate utf8mb4_bin",
                "`provider_user_id`   varchar(128) character set utf8mb4 collate utf8mb4_bin"
        );
    }

    /**
     * 增量脚本只能补表或迁移Step 1密码列，且认证表不得保存 OAuth 凭证或建立数据库外键。
     *
     * @throws IOException 脚本读取异常
     */
    @Test
    void upgradeAndCredentialBoundariesShouldRemainSafe() throws IOException {
        String initSql = readRepositoryFile(INIT_SQL).toUpperCase(Locale.ROOT);
        String upgradeSql = readRepositoryFile(UPGRADE_SQL).toUpperCase(Locale.ROOT);

        assertThat(upgradeSql).contains(
                "CREATE TABLE IF NOT EXISTS `APP_USER`",
                "CREATE TABLE IF NOT EXISTS `APP_OAUTH_ACCOUNT`",
                "ALTER TABLE `APP_USER` CHANGE COLUMN `PASSWORD_HASH` `PASSWORD`"
        );
        assertThat(upgradeSql).doesNotContain(
                "DROP TABLE", "TRUNCATE TABLE", "DELETE FROM", "INSERT INTO", "UPDATE `"
        );
        assertThat(initSql + upgradeSql).doesNotContain(
                " FOREIGN KEY ",
                " JOIN ",
                "`ACCESS_TOKEN`",
                "`REFRESH_TOKEN`",
                "`CLIENT_SECRET`",
                "`RAW_PROFILE`",
                "`EMAIL`"
        );
        assertThat(initSql.indexOf("DROP TABLE IF EXISTS `APP_OAUTH_ACCOUNT`"))
                .isLessThan(initSql.indexOf("DROP TABLE IF EXISTS `APP_USER`"));
    }

    private List<String> extractColumns(String sql, String tableName) {
        String ddl = extractCreateTable(sql, tableName);
        Matcher matcher = Pattern.compile("(?m)^\\s*`([a-z_]+)`\\s+").matcher(ddl);
        List<String> columns = new ArrayList<>();
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return columns;
    }

    private String extractCreateTable(String sql, String tableName) {
        Pattern pattern = Pattern.compile(
                "(?is)CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`" + Pattern.quote(tableName)
                        + "`\\s*\\(.*?\\)\\s*ENGINE\\s*=\\s*InnoDB.*?;"
        );
        Matcher matcher = pattern.matcher(sql);
        assertThat(matcher.find()).as("缺少表 DDL：%s", tableName).isTrue();
        return matcher.group();
    }

    private String normalizeDdl(String ddl) {
        return ddl.toLowerCase(Locale.ROOT)
                .replaceFirst("create\\s+table\\s+if\\s+not\\s+exists", "create table")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String readRepositoryFile(String relativePath) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库文件：" + relativePath);
    }
}
