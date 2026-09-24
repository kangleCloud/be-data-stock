package com.vita.redis.property;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.redis.property
 * @Author: znk
 * @CreateTime: 2026-03-02  17:25:47
 * @Description: RedissonRroperty Redisson 配置属性类
 * @Version: 1.0
 */
@Data
@Configuration
public class RedissonProperty {

    /**
     * redis连接ip
     */
    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;
    /**
     * redis连接端口
     */
    @Value("${spring.data.redis.port:6379}")
    private Integer port;
    /**
     * redis连接密码
     */
    @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * redis数据库索引
     */
    @Value("${spring.data.redis.database:0}")
    private Integer database;

    /**
     * 是否开启redis
     */
    @Value("${spring.data.redis.open:}")
    private Boolean open;


}
