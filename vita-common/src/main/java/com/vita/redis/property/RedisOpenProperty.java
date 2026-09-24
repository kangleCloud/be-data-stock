package com.vita.redis.property;

import com.vita.utils.spring.SpringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.redis.property
 * @Author: znk
 * @CreateTime: 2026-03-12  16:00:05
 * @Description: RedisOpenProperty Redis开放属性配置类
 * @Version: 1.0
 */
@Configuration
public class RedisOpenProperty implements InitializingBean {
    /**
     * 是否开启redis
     */
    @Value("${spring.data.redis.open:false}")
    private Boolean open;

    /**
     * redis模板
     */
    private static RedisTemplate redisTemplate;

    /**
     * 获取redis模板
     */
    public static RedisTemplate getResTemplate() {
        return redisTemplate;
    }

    /**
     * 属性后的初始化Bean
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        redisTemplate = SpringUtils.getBean(open, "redisTemplate");
    }
}
