package com.vita.system.sysOperLog.support;

import com.vita.log.model.OperLogRecord;
import com.vita.log.persistence.OperLogPersistence;
import com.vita.system.sysOperLog.dto.SysOperLogCreateDto;
import com.vita.system.sysOperLog.service.ISysOperLogService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 操作日志持久化适配器，将 common 层采集结果转换为系统操作日志实体。
 *
 * @author znk
 */
@Component
public class SysOperLogPersistenceHandler implements OperLogPersistence {

    @Resource
    private ISysOperLogService sysOperLogService;

    /**
     * 持久化审计日志。
     *
     * @param record 审计日志记录
     */
    @Override
    public void persist(OperLogRecord record) {
        if (record == null) {
            return;
        }

        SysOperLogCreateDto createDto = new SysOperLogCreateDto();
        BeanUtils.copyProperties(record, createDto);
        sysOperLogService.create(createDto);
    }
}
