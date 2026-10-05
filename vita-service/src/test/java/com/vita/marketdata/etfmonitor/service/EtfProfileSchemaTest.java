package com.vita.marketdata.etfmonitor.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile;
import com.vita.marketdata.etfmonitor.mapper.EtfMonitorProfileMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EtfProfileSchemaTest {
    @Test
    void nullableColumnsMatchEntityAndRebuildMatchesInitialization() throws Exception {
        Path root = Path.of("..");
        String init = Files.readString(root.resolve("sql/init/system.sql"));
        String initialUpgrade = Files.readString(root.resolve("sql/upgrade/20261002_etf_monitor_v1.sql"));
        String upgrade = Files.readString(root.resolve("sql/upgrade/20261003_etf_profile_ths.sql"));
        for (var field : Map.of("fullName", "full_name", "fundType", "fund_type",
                "investmentType", "investment_type", "fundManager", "fund_manager",
                "establishedDate", "established_date", "performanceBenchmark", "performance_benchmark",
                "source", "source").entrySet()) {
            assertNotNull(EtfMonitorProfile.class.getDeclaredField(field.getKey()));
            for (String sql : new String[]{init, initialUpgrade, upgrade}) {
                String table = sql.substring(sql.indexOf("`etf_monitor_profile` ("));
                table = table.substring(0, table.indexOf(") ENGINE="));
                assertTrue(table.matches("(?s).*`" + field.getValue() + "` [^\n]*DEFAULT NULL.*"));
            }
        }
        assertTrue(upgrade.contains("DROP TABLE IF EXISTS `etf_monitor_profile`;"));
        assertEquals(tableDefinition(init), tableDefinition(upgrade));
        assertEquals(tableDefinition(initialUpgrade), tableDefinition(upgrade));
        assertFalse(upgrade.contains("INSERT INTO"));
        assertFalse(upgrade.contains("ALTER TABLE"));
        var drops = java.util.regex.Pattern.compile("DROP TABLE IF EXISTS `([^`]+)`").matcher(upgrade);
        assertTrue(drops.find());
        assertEquals("etf_monitor_profile", drops.group(1));
        assertFalse(drops.find());
    }

    private String tableDefinition(String sql) {
        int start = sql.indexOf("`etf_monitor_profile` (");
        return sql.substring(start, sql.indexOf(';', start) + 1);
    }

    @Test
    void replacementSqlBindsNullableThsValuesAndClearsUnsupportedExchangeColumns() {
        var configuration = new MybatisConfiguration();
        configuration.addMapper(EtfMonitorProfileMapper.class);
        var row = new EtfMonitorProfile();
        row.setId(1L); row.setSymbol("SH510050"); row.setSource("THS");
        var bound = configuration.getMappedStatement(EtfMonitorProfileMapper.class.getName() + ".updateThsProfile")
                .getBoundSql(row);
        for (String field : new String[]{"listing_status", "listing_date", "share_count", "share_date"}) {
            assertTrue(bound.getSql().contains(field + "=NULL"));
        }
        assertTrue(bound.getParameterMappings().stream().anyMatch(parameter -> parameter.getProperty().equals("fullName")));
        assertTrue(bound.getParameterMappings().stream().anyMatch(parameter -> parameter.getProperty().equals("profileUpdatedAt")));
        assertTrue(bound.getSql().contains("is_deleted=0"));
    }
}
