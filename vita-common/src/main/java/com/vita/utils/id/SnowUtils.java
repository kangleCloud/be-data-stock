package com.vita.utils.id;

import cn.hutool.core.util.IdUtil;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.id
 * @Author: znk
 * @CreateTime: 2026-03-12  20:49:04
 * @Description: 雪花算法工具类，封装了生成唯一ID的方法，使用Hutool的IdUtil实现
 * @Version: 1.0
 */
public class SnowUtils {

    private SnowUtils() {
        // 私有化构造函数，防止实例化
    }

    /**
     * 数据中心ID
     */
    private static final long dataCenterId = 1;

    /**
     * 工作节点ID
     */
    private static final long workerId = 1;

    public static long getSnowflakeNextId() {
        return IdUtil.getSnowflake(dataCenterId, workerId).nextId();
    }

    public static String getSnowflakeNextIdStr() {
        return IdUtil.getSnowflake(dataCenterId, workerId).nextIdStr();
    }
}
