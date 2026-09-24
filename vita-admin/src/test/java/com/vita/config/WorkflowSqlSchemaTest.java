package com.vita.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-07-14
 * @Description: Warm-Flow 数据库脚本回归测试
 * @Version: 1.0
 */
class WorkflowSqlSchemaTest {

    private static final Pattern FLOW_TABLE_PATTERN = Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+`(flow_[a-z_]+)`"
    );

    private static final Pattern FLOW_DROP_PATTERN = Pattern.compile(
            "(?i)DROP\\s+TABLE\\s+IF\\s+EXISTS\\s+`(flow_[a-z_]+)`"
    );

    private static final Set<String> REQUIRED_FLOW_TABLES = Set.of(
            "flow_definition",
            "flow_node",
            "flow_skip",
            "flow_instance",
            "flow_task",
            "flow_his_task",
            "flow_user",
            "flow_form"
    );

    /**
     * 初始化脚本必须包含 Warm-Flow 1.8.8 运行所需表。
     *
     * @throws IOException 脚本读取异常
     */
    @Test
    void initScriptShouldContainAllWarmFlowTables() throws IOException {
        String sql = Files.readString(resolveRepositoryFile("sql/init/workflow.sql"));
        Matcher matcher = FLOW_TABLE_PATTERN.matcher(sql);
        Set<String> tableNames = new java.util.HashSet<>();
        while (matcher.find()) {
            tableNames.add(matcher.group(1).toLowerCase());
        }

        assertThat(tableNames).containsExactlyInAnyOrderElementsOf(REQUIRED_FLOW_TABLES);
    }

    /**
     * 引擎升级脚本必须删除并按初始化 DDL 重建全部 Warm-Flow 表。
     *
     * @throws IOException 脚本读取异常
     */
    @Test
    void upgradeScriptShouldRebuildAllWarmFlowTablesFromInitDdl() throws IOException {
        String initSql = Files.readString(resolveRepositoryFile("sql/init/workflow.sql"));
        String upgradeSql = Files.readString(resolveRepositoryFile("sql/upgrade/warm_flow_upgrade.sql"));

        assertThat(findTableNames(FLOW_TABLE_PATTERN, upgradeSql))
                .containsExactlyInAnyOrderElementsOf(REQUIRED_FLOW_TABLES);
        assertThat(findTableNames(FLOW_DROP_PATTERN, upgradeSql))
                .containsExactlyInAnyOrderElementsOf(REQUIRED_FLOW_TABLES);
        for (String tableName : REQUIRED_FLOW_TABLES) {
            assertThat(normalizeDdl(extractCreateTable(upgradeSql, tableName)))
                    .isEqualTo(normalizeDdl(extractCreateTable(initSql, tableName)));
        }

        assertThat(upgradeSql).contains("Warning:", "vita_workflow_upgrade.sql");
        assertThat(upgradeSql).doesNotContain(
                "uk_flow_instance_business_id",
                "idx_flow_instance_create_by_update",
                "idx_flow_task_instance",
                "idx_flow_his_task_approver_update"
        );
    }

    private Set<String> findTableNames(Pattern pattern, String sql) {
        Matcher matcher = pattern.matcher(sql);
        Set<String> tableNames = new java.util.HashSet<>();
        while (matcher.find()) {
            tableNames.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return tableNames;
    }

    private String extractCreateTable(String sql, String tableName) {
        Pattern pattern = Pattern.compile(
                "(?is)CREATE\\s+TABLE\\s+`" + Pattern.quote(tableName)
                        + "`\\s*\\(.*?\\)\\s*ENGINE\\s*=\\s*InnoDB.*?;"
        );
        Matcher matcher = pattern.matcher(sql);
        assertThat(matcher.find()).as("缺少表 DDL：%s", tableName).isTrue();
        return matcher.group();
    }

    private String normalizeDdl(String ddl) {
        return ddl.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private Path resolveRepositoryFile(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库文件：" + relativePath);
    }
}
