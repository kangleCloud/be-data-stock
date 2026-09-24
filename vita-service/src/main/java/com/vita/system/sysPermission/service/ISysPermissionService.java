package com.vita.system.sysPermission.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.system.sysPermission.dto.*;
import com.vita.system.sysPermission.entity.SysPermission;
import com.vita.system.sysPermission.vo.SysPermissionDetailVo;
import com.vita.system.sysPermission.vo.SysPermissionListVo;
import com.vita.system.sysPermission.vo.SysPermissionOptionVo;
import com.vita.system.sysPermission.vo.SysPermissionPageVo;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.service
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 Service 接口
 * @version 1.0
 */
public interface ISysPermissionService extends IService<SysPermission> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysPermissionCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysPermissionUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(SysPermissionDeletedDto deletedDto);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysPermissionDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysPermissionListVo> list(SysPermissionSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysPermissionPageVo> page(SysPermissionSearchDto searchDto);

    /**
     * 查询全部权限。
     *
     * @return 权限列表
     */
    List<SysPermissionListVo> getAll();

    /**
     * 根据权限ID和状态查询权限列表。
     *
     * @param permissionIds 权限ID列表
     * @param status 权限状态，可为空
     * @return 权限列表
     */
    List<SysPermission> getListByIds(List<Long> permissionIds, Byte status);

    /**
     * 根据角色ID查询权限列表。
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<SysPermissionListVo> getListByRoleId(Long roleId);

    /**
     * 根据角色ID获取权限选择框列表。
     *
     * @param roleId 角色ID
     * @return 权限选择框列表
     */
    List<SysPermissionOptionVo> getOptionListByRoleId(Long roleId);

    /**
     * 校验权限编码是否唯一。
     *
     * @param permission 权限信息
     * @return true 唯一 / false 不唯一
     */
    boolean checkPermissionCodeUnique(SysPermission permission);

    /**
     * 校验鉴权标识是否唯一。
     *
     * @param permission 权限信息
     * @return true 唯一 / false 不唯一
     */
    boolean checkAuthTagUnique(SysPermission permission);

    /**
     * 校验权限是否允许操作。
     *
     * @param permissionId 权限ID
     */
    void checkPermissionAllowed(Long permissionId);

    /**
     * 批量删除权限。
     *
     * @param batchDeletedDto 批量删除请求对象
     */
    void deleteBatch(SysPermissionBatchDeletedDto batchDeletedDto);

    /**
     * 根据权限ID和状态查询权限码集合。
     *
     * @param permissionIds 权限ID集合
     * @param status 权限状态，可为空
     * @return 权限码集合
     */
    List<String> getPermissionCodesByIds(List<Long> permissionIds, Byte status);
}
