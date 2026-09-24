package com.vita.log.persistence;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.persistence
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 登录归属地持久化扩展接口
 * @Version: 1.0
 */
public interface LoginLocationPersistence {

    /**
     * 回填登录日志归属地。
     *
     * @param logId    登录日志主键ID
     * @param location 归属地
     */
    void updateLoginLocation(Long logId, String location);
}
