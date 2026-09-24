package com.vita.system.sysPermission.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.sysPermission.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.mapper
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface SysPermissionMapper extends BaseMapperX<SysPermission> {

    /**
     * 根据权限ID和状态查询权限列表。
     *
     * @param permissionIds 权限ID列表，可为空
     * @param status 权限状态，可为空
     * @return 权限列表
     */
    default List<SysPermission> selectListByIdsAndStatus(List<Long> permissionIds, Byte status) {
        return selectList(new LambdaQueryWrapperX<SysPermission>()
                .inIfPresent(SysPermission::getId, permissionIds)
                .eqIfPresent(SysPermission::getStatus, status)
                .orderByAsc(SysPermission::getSortNo, SysPermission::getId));
    }

    /**
     * 判断权限编码是否已存在。
     *
     * @param permissionCode 权限编码
     * @param excludeId 排除的权限ID，可为空
     * @return true 已存在
     */
    default boolean existsByPermissionCode(String permissionCode, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<SysPermission>()
                .eq(SysPermission::getPermissionCode, permissionCode)
                .neIfPresent(SysPermission::getId, excludeId)) > 0;
    }

    /**
     * 判断鉴权标识是否已存在。
     *
     * @param authTag 鉴权标识
     * @param excludeId 排除的权限ID，可为空
     * @return true 已存在
     */
    default boolean existsByAuthTag(String authTag, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<SysPermission>()
                .eq(SysPermission::getAuthTag, authTag)
                .neIfPresent(SysPermission::getId, excludeId)) > 0;
    }
}
