package com.vita.system.associate.sysRolePermission.service;

import com.vita.core.page.PageResponse;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionCreateDto;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionSearchDto;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionUpdateDto;
import com.vita.system.associate.sysRolePermission.entity.SysRolePermission;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionDetailVo;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionListVo;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRolePermission.service
 * @Author znk
 * @CreateTime 2026-03-31 16:40:54
 * @Description: 角色权限关联表 Service 接口
 * @version 1.0
 */
public interface ISysRolePermissionService extends IService<SysRolePermission> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysRolePermissionCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysRolePermissionUpdateDto updateDto);

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
    SysRolePermissionDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysRolePermissionListVo> list(SysRolePermissionSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysRolePermissionPageVo> page(SysRolePermissionSearchDto searchDto);

    /**
     * 查询角色权限ID集合。
     *
     * @param roleIds 角色ID集合
     * @return 权限ID集合
     */
    List<Long> getPermissionIdsByRoleIds(List<Long> roleIds);

    /**
     * 查询单个角色权限ID集合。
     *
     * @param roleId 角色ID
     * @return 权限ID集合
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 根据角色ID列表删除角色权限关联。
     *
     * @param roleIds 角色ID集合
     */
    void removeByRoleIds(List<Long> roleIds);

    /**
     * 根据权限ID列表删除角色权限关联。
     *
     * @param permissionIds 权限ID集合
     */
    void removeByPermissionIds(List<Long> permissionIds);

    /**
     * 根据角色ID和权限ID列表删除角色权限关联。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    void removeByRoleIdAndPermissionIds(Long roleId, List<Long> permissionIds);

    /**
     * 批量为角色分配权限。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    void assignPermissionsToRole(Long roleId, List<Long> permissionIds);
}
