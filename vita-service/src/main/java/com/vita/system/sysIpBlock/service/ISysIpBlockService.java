package com.vita.system.sysIpBlock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.system.sysIpBlock.dto.SysIpBlockCreateDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockDeletedDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockSearchDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockUpdateDto;
import com.vita.system.sysIpBlock.entity.SysIpBlock;
import com.vita.system.sysIpBlock.vo.SysIpBlockDetailVo;
import com.vita.system.sysIpBlock.vo.SysIpBlockListVo;
import com.vita.system.sysIpBlock.vo.SysIpBlockPageVo;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.service
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 Service 接口
 */
public interface ISysIpBlockService extends IService<SysIpBlock> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysIpBlockCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysIpBlockUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(SysIpBlockDeletedDto deletedDto);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysIpBlockDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysIpBlockListVo> list(SysIpBlockSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysIpBlockPageVo> page(SysIpBlockSearchDto searchDto);

    /**
     * 判断指定 IP 是否被封禁。
     *
     * @param ip IP地址
     * @return true-已封禁
     */
    boolean isBlocked(String ip);

    /**
     * 启动时装载生效封禁IP到缓存。
     */
    void loadBlockedIpCache();
}
