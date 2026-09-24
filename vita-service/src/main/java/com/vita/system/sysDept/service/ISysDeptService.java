package com.vita.system.sysDept.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.system.sysDept.dto.SysDeptCreateDto;
import com.vita.system.sysDept.dto.SysDeptDeletedDto;
import com.vita.system.sysDept.dto.SysDeptSearchDto;
import com.vita.system.sysDept.dto.SysDeptUpdateDto;
import com.vita.system.sysDept.entity.SysDept;
import com.vita.system.sysDept.vo.SysDeptDetailVo;
import com.vita.system.sysDept.vo.SysDeptListVo;
import com.vita.system.sysDept.vo.SysDeptPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysDept.service
 * @Author znk
 * @CreateTime 2026-03-31 16:29:38
 * @Description: 部门表 Service 接口
 * @version 1.0
 */
public interface ISysDeptService extends IService<SysDept> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysDeptCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysDeptUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(SysDeptDeletedDto deletedDto);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysDeptDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysDeptListVo> list(SysDeptSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysDeptPageVo> page(SysDeptSearchDto searchDto);
}
