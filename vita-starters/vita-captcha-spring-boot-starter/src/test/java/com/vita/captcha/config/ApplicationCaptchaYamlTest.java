package com.vita.captcha.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证码默认配置回归测试。
 *
 * @author znk
 */
class ApplicationCaptchaYamlTest {

    @Test
    void adminCaptchaShouldDefaultToEnabledCharacterImage() throws IOException {
        PropertySource<?> propertySource = new YamlPropertySourceLoader()
                .load("application-captcha.yml", new ClassPathResource("application-captcha.yml"))
                .get(0);

        assertThat(propertySource.getProperty("vita.captcha.enabled")).isEqualTo(true);
        assertThat(propertySource.getProperty("vita.captcha.captcha-type")).isEqualTo("char");
        assertThat(propertySource.getProperty("vita.captcha.expired")).isEqualTo(3);
    }
}
