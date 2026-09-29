package com.vita.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDashboardAdminBoundaryTest {

    @Test
    void adminRuntimeDoesNotContainDashboardController() {
        assertThat(getClass().getClassLoader().getResource(
                "com/vita/controller/market/MarketDashboardController.class")).isNull();
        assertThat(getClass().getClassLoader().getResource(
                "com/vita/config/market/MarketSnapshotStreamConfiguration.class")).isNull();
    }
}
