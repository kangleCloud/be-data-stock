package com.vita.system.sysOperLog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.system.sysOperLog.dto.SysOperLogCreateDto;
import com.vita.system.sysOperLog.dto.SysOperLogDeletedDto;
import com.vita.system.sysOperLog.dto.SysOperLogSearchDto;
import com.vita.system.sysOperLog.dto.SysOperLogUpdateDto;
import com.vita.system.sysOperLog.entity.SysOperLog;
import com.vita.system.sysOperLog.vo.SysOperLogDetailVo;
import com.vita.system.sysOperLog.vo.SysOperLogListVo;
import com.vita.system.sysOperLog.vo.SysOperLogPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysOperLog.service
 * @Author znk
 * @CreateTime 2026-03-12 21:45:20
 * @Description: 系统操作日志表 Service 接口
 * @version 1.0
 */
public interface ISysOperLogService extends IService<SysOperLog> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysOperLogCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysOperLogUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(SysOperLogDeletedDto deletedDto);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysOperLogDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysOperLogListVo> list(SysOperLogSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysOperLogPageVo> page(SysOperLogSearchDto searchDto);
}
