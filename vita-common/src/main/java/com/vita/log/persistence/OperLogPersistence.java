package com.vita.log.persistence;

import com.vita.log.model.OperLogRecord;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.persistence
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 操作日志持久化扩展接口
 * @Version: 1.0
 */
public interface OperLogPersistence {

    /**
     * 持久化审计日志。
     *
     * @param record 审计日志记录
     */
    void persist(OperLogRecord record);
}
