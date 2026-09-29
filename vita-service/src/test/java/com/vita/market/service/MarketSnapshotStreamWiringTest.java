package com.vita.market.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSnapshotStreamWiringTest {

    @Test
    void springCreatesStreamServiceWithSnapshotDependency() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(MarketSnapshotService.class, () -> () -> null);
            context.register(MarketSnapshotStreamService.class);
            context.refresh();
            assertThat(context.getBean(MarketSnapshotStreamService.class)).isNotNull();
        }
    }
}
