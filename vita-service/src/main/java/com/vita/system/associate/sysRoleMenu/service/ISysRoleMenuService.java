package com.vita.system.associate.sysRoleMenu.service;

import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuCreateDto;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuSearchDto;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuUpdateDto;
import com.vita.system.associate.sysRoleMenu.entity.SysRoleMenu;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuDetailVo;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuListVo;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuPageVo;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleMenu.service
 * @Author znk
 * @CreateTime 2026-03-31 16:40:21
 * @Description: 角色菜单关联表 Service 接口
 */
public interface ISysRoleMenuService extends IService<SysRoleMenu> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysRoleMenuCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysRoleMenuUpdateDto updateDto);

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
    SysRoleMenuDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysRoleMenuListVo> list(SysRoleMenuSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysRoleMenuPageVo> page(SysRoleMenuSearchDto searchDto);

    /**
     * 查询角色菜单ID集合。
     *
     * @param roleIds 角色ID集合
     * @return 菜单ID集合
     */
    List<Long> getMenuIdsByRoleIds(List<Long> roleIds);

    /**
     * 根据角色ID列表删除角色菜单关联。
     *
     * @param roleIds 角色ID集合
     */
    void removeByRoleIds(List<Long> roleIds);
}
