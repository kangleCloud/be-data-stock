package com.vita.system.sysRole.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.system.sysRole.dto.*;
import com.vita.system.sysRole.entity.SysRole;
import com.vita.system.sysRole.vo.SysRoleDetailVo;
import com.vita.system.sysRole.vo.SysRoleListVo;
import com.vita.system.sysRole.vo.SysRoleOptionVo;
import com.vita.system.sysRole.vo.SysRolePageVo;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.service
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 Service 接口
 */
public interface ISysRoleService extends IService<SysRole> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysRoleCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysRoleUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(SysRoleDeletedDto deletedDto);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysRoleDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysRoleListVo> list(SysRoleSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysRolePageVo> page(SysRoleSearchDto searchDto);

    /**
     * 根据角色ID和状态列表查询角色信息。
     *
     * @param roleIds 角色ID列表
     * @param status 角色状态（可选）
     * @return 角色信息列表
     */
    List<SysRole> getListByIds(List<Long> roleIds, Byte status);

    /**
     * 根据角色ID和状态列表查询角色编码。
     *
     * @param roleIds 角色ID列表
     * @param status 角色状态（可选）
     * @return 角色编码列表
     */
    List<String> getRoleCodesByIds(List<Long> roleIds, Byte status);

    /**
     * 根据用户ID查询角色列表。
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRoleListVo> getListByUserId(Long userId);

    /**
     * 查询全部角色。
     *
     * @return 角色列表
     */
    List<SysRoleListVo> getAll();

    /**
     * 根据用户ID获取角色选择框列表。
     *
     * @param userId 用户ID
     * @return 角色选择框列表
     */
    List<SysRoleOptionVo> getOptionListByUserId(Long userId);

    /**
     * 校验角色名称是否唯一。
     *
     * @param role 角色信息
     * @return true 唯一 / false 不唯一
     */
    boolean checkRoleNameUnique(SysRole role);

    /**
     * 校验角色编码是否唯一。
     *
     * @param role 角色信息
     * @return true 唯一 / false 不唯一
     */
    boolean checkRoleCodeUnique(SysRole role);

    /**
     * 校验角色是否允许操作。
     *
     * @param roleId 角色ID
     */
    void checkRoleAllowed(Long roleId);

    /**
     * 批量删除角色。
     *
     * @param batchDeletedDto 批量删除请求对象
     */
    void deleteBatch(SysRoleBatchDeletedDto batchDeletedDto);

    /**
     * 取消授权单个用户角色。
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     */
    void cancelAuthUser(Long roleId, Long userId);

    /**
     * 批量取消授权用户角色。
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    void cancelAuthUsers(Long roleId, List<Long> userIds);

    /**
     * 批量选择授权用户角色。
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    void authUsers(Long roleId, List<Long> userIds);

    /**
     * 取消授权单个角色权限。
     *
     * @param roleId 角色ID
     * @param permissionId 权限ID
     */
    void cancelAuthPermission(Long roleId, Long permissionId);

    /**
     * 批量取消授权角色权限。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    void cancelAuthPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 批量选择授权角色权限。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    void authPermissions(Long roleId, List<Long> permissionIds);
}
