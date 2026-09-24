package com.vita.system.sysLoginLog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.system.sysLoginLog.dto.SysLoginLogCreateDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogDeletedDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogSearchDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogUpdateDto;
import com.vita.system.sysLoginLog.entity.SysLoginLog;
import com.vita.system.sysLoginLog.vo.SysLoginLogDetailVo;
import com.vita.system.sysLoginLog.vo.SysLoginLogListVo;
import com.vita.system.sysLoginLog.vo.SysLoginLogPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysLoginLog.service
 * @Author znk
 * @CreateTime 2026-03-21 00:20:48
 * @Description: 系统访问记录 Service 接口
 * @version 1.0
 */
public interface ISysLoginLogService extends IService<SysLoginLog> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysLoginLogCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysLoginLogUpdateDto updateDto);

    /**
     * 仅更新登录地点。
     *
     * @param id 主键ID
     * @param loginLocation 登录地点
     */
    void updateLoginLocation(Long id, String loginLocation);

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(SysLoginLogDeletedDto deletedDto);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysLoginLogDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysLoginLogListVo> list(SysLoginLogSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysLoginLogPageVo> page(SysLoginLogSearchDto searchDto);
}
