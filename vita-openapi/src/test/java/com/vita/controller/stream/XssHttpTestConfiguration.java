package com.vita.controller.stream;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** 本地随机端口测试：复用模拟业务与生产鉴权，表单和上传交由真实 Servlet 容器。 */
@Configuration(proxyBeanMethods = false)
@Import({SseHttpTestConfiguration.class, XssEchoTestController.class})
@ImportAutoConfiguration({HttpEncodingAutoConfiguration.class, MultipartAutoConfiguration.class})
public class XssHttpTestConfiguration {
    @Bean
    OrderedFormContentFilter formContentFilter() {
        return new OrderedFormContentFilter();
    }
}
