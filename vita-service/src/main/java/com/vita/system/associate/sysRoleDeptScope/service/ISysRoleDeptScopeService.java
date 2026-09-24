package com.vita.system.associate.sysRoleDeptScope.service;

import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeCreateDto;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeSearchDto;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeUpdateDto;
import com.vita.system.associate.sysRoleDeptScope.entity.SysRoleDeptScope;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopeDetailVo;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopeListVo;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopePageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleDeptScope.service
 * @Author znk
 * @CreateTime 2026-03-31 16:41:38
 * @Description: 角色自定义数据范围表 Service 接口
 * @version 1.0
 */
public interface ISysRoleDeptScopeService extends IService<SysRoleDeptScope> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysRoleDeptScopeCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysRoleDeptScopeUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysRoleDeptScopeDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysRoleDeptScopeListVo> list(SysRoleDeptScopeSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysRoleDeptScopePageVo> page(SysRoleDeptScopeSearchDto searchDto);

    /**
     * 根据角色ID列表删除角色自定义数据范围关联。
     *
     * @param roleIds 角色ID集合
     */
    void removeByRoleIds(List<Long> roleIds);
}
