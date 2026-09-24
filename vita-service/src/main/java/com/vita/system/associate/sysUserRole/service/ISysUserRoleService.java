package com.vita.system.associate.sysUserRole.service;

import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleCreateDto;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleSearchDto;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleUpdateDto;
import com.vita.system.associate.sysUserRole.entity.SysUserRole;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.system.associate.sysUserRole.vo.SysUserRoleDetailVo;
import com.vita.system.associate.sysUserRole.vo.SysUserRoleListVo;
import com.vita.system.associate.sysUserRole.vo.SysUserRolePageVo;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysUserRole.service
 * @Author znk
 * @CreateTime 2026-03-31 16:39:20
 * @Description: 用户角色关联表 Service 接口
 */
public interface ISysUserRoleService extends IService<SysUserRole> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysUserRoleCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysUserRoleUpdateDto updateDto);

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
    SysUserRoleDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysUserRoleListVo> list(SysUserRoleSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysUserRolePageVo> page(SysUserRoleSearchDto searchDto);

    /**
     * 根据用户ID获取其拥有的角色ID列表。
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> getRoleIdsByUserId(Long userId);

    /**
     * 根据角色ID获取其拥有的用户ID列表。
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<Long> getUserIdsByRoleId(Long roleId);

    /**
     * 根据角色ID列表删除用户角色关联。
     *
     * @param roleIds 角色ID列表
     */
    void removeByRoleIds(List<Long> roleIds);

    /**
     * 根据角色ID和用户ID列表删除用户角色关联。
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    void removeByRoleIdAndUserIds(Long roleId, List<Long> userIds);

    /**
     * 批量为用户分配角色。
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    void assignRoleToUsers(Long roleId, List<Long> userIds);

}
