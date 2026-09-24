package com.vita.config;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-03-02
 * @Description: vita admin 项目启动类
 * @Version: 1.0
 */
@SpringBootApplication(
        scanBasePackages = "com.vita")
@MapperScan("com.vita.**.mapper")
@EnableTransactionManagement
public class VitaAdminApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(VitaAdminApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(VitaAdminApplication.class);
        ConfigurableEnvironment env = app.run(args).getEnvironment();
        LOGGER.info("http-port:{}", env.getProperty("server.port"));
    }

}
