package com.vita.web.xss;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vita.web.xss.property.XssProperty;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 单一注册入口；Filter 不标注组件，避免容器重复装配。 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(XssProperty.class)
public class XssConfig {
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration(XssProperty property, ObjectMapper mapper) {
        var registration = new FilterRegistrationBean<>(new XssFilter(property, mapper));
        registration.setName("xssFilter");
        registration.setEnabled(property.isEnabled());
        registration.setUrlPatterns(property.getUrlPatterns());
        registration.setOrder(OrderedFormContentFilter.DEFAULT_ORDER + 1);
        // 编码及 FormContentFilter 之后、MVC 之前，覆盖 PUT/PATCH/DELETE 的表单参数。
        // 只检查首次请求；SSE 异步完成不会重复读体。
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setAsyncSupported(true);
        return registration;
    }
}
