package com.vita.marketdata;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

/** 独立 Mapper 接口也纳入扫描，验证包迁移后的 MyBatis 声明。 */
final class MarketDataMapperScanner extends ClassPathScanningCandidateComponentProvider {
    MarketDataMapperScanner() { super(false); }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
        return definition.getMetadata().isIndependent();
    }
}
