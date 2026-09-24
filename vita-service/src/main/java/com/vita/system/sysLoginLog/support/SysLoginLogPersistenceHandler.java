package com.vita.system.sysLoginLog.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.log.model.LoginAuditRecord;
import com.vita.log.persistence.LoginAuditPersistence;
import com.vita.log.persistence.LoginLocationPersistence;
import com.vita.system.sysLoginLog.dto.SysLoginLogCreateDto;
import com.vita.system.sysLoginLog.service.ISysLoginLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 登录审计持久化适配器，将 common 层登录审计快照转换为系统登录日志实体。
 *
 * @author znk
 */
@Component
public class SysLoginLogPersistenceHandler implements LoginAuditPersistence, LoginLocationPersistence {

    @Resource
    private ISysLoginLogService sysLoginLogService;

    /**
     * 持久化登录审计记录。
     *
     * @param record 登录审计记录
     */
    @Override
    public Long persist(LoginAuditRecord record) {
        if (record == null) {
            return null;
        }

        SysLoginLogCreateDto createDto = new SysLoginLogCreateDto();
        createDto.setUserName(record.getUserName());
        createDto.setIpaddr(record.getIpaddr());
        createDto.setLoginLocation(record.getLoginLocation());
        createDto.setBrowser(record.getBrowser());
        createDto.setOs(record.getOs());
        createDto.setStatus(record.getStatus());
        createDto.setMsg(record.getMsg());
        createDto.setLoginTime(record.getLoginTime());
        return sysLoginLogService.create(createDto);
    }

    @Override
    public void updateLoginLocation(Long logId, String location) {
        if (logId == null || CharSequenceUtil.isBlank(location)) {
            return;
        }
        sysLoginLogService.updateLoginLocation(logId, location);
    }
}
