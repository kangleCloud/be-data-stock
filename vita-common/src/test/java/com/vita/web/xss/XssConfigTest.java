package com.vita.web.xss;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.web.xss.property.XssProperty;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;

import static org.junit.jupiter.api.Assertions.*;

class XssConfigTest {
    @Test
    void servletContextRegistersOnlyOnceWithConfiguredPatternsAndSafeOrder() {
        new WebApplicationContextRunner().withUserConfiguration(XssConfig.class)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues("vita.xss.url-patterns[0]=/api/*", "vita.xss.exclude-paths[0]=/trusted")
                .run(context -> {
                    assertEquals(1,context.getBeansOfType(FilterRegistrationBean.class).size());
                    assertEquals(0,context.getBeansOfType(XssFilter.class).size(), "Filter 不单独注册为组件");
                    var registration = context.getBean(FilterRegistrationBean.class);
                    assertEquals(java.util.Set.of("/api/*"), java.util.Set.copyOf(registration.getUrlPatterns()));
                    assertTrue(registration.isEnabled());
                    assertTrue(registration.getOrder() > new OrderedCharacterEncodingFilter().getOrder());
                    assertTrue(registration.getOrder() > OrderedFormContentFilter.DEFAULT_ORDER);
                    assertEquals(java.util.List.of("/trusted"),context.getBean(XssProperty.class).getExcludePaths());
                });
    }

    @Test
    void enabledFalseDisablesRegistration() {
        new WebApplicationContextRunner().withUserConfiguration(XssConfig.class)
                .withBean(ObjectMapper.class,ObjectMapper::new).withPropertyValues("vita.xss.enabled=false")
                .run(context -> assertFalse(context.getBean(FilterRegistrationBean.class).isEnabled()));
    }
}
