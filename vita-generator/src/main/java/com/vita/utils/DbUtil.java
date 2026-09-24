package com.vita.utils;

import com.baomidou.mybatisplus.generator.config.DataSourceConfig;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.utils
 * @Author: znk
 * @CreateTime: 2026-03-04  14:01:47
 * @Description: 数据库工具类
 * @Version: 1.0
 */
public class DbUtil {

    /**
     * 获取数据库连接信息
     *
     * @param dbHost 数据库主机
     * @param dbPort 数据库端口
     * @param dbName 数据库名称
     * @param dbUser 数据库用户名
     * @param dbPass 数据库密码
     * @return 数据源配置
     */
    public static DataSourceConfig.Builder getDataSourceConfig(String dbHost, Integer dbPort, String dbName, String dbUser, String dbPass) {
        String url = String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false",
                dbHost, dbPort, dbName
        );
        return new DataSourceConfig
                .Builder(url, dbUser, dbPass)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .schema(dbName);
    }
}
