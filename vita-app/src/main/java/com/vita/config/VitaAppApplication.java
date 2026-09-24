package com.vita.config;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: Vita App启动类
 * @Version: 1.0
 */
@MapperScan("com.vita.**.mapper")
@SpringBootApplication(scanBasePackages = "com.vita")
public class VitaAppApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(VitaAppApplication.class);


    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(VitaAppApplication.class);
        ConfigurableEnvironment env = app.run(args).getEnvironment();
        LOGGER.info("http-port:{}", env.getProperty("server.port"));
    }
}
