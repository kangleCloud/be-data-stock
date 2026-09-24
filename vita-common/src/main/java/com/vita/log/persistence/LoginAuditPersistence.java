package com.vita.log.persistence;

import com.vita.log.model.LoginAuditRecord;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.persistence
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 登录审计持久化扩展接口
 * @Version: 1.0
 */
public interface LoginAuditPersistence {

    /**
     * 持久化登录审计记录。
     *
     * @param record 登录审计记录
     * @return 登录日志主键ID；未落库时返回 null
     */
    Long persist(LoginAuditRecord record);
}
