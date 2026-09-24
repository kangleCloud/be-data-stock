package com.vita.system.sysRole.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.sysRole.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.mapper
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface SysRoleMapper extends BaseMapperX<SysRole> {

    /**
     * 根据角色ID和状态查询角色编码集合。
     *
     * @param roleIds 角色ID集合
     * @param status  角色状态，可为空
     * @return 角色编码集合
     */
    List<String> selectRoleCodesByIds(@Param("roleIds") List<Long> roleIds, @Param("status") Byte status);

    /**
     * 根据角色ID和状态查询角色集合。
     *
     * @param roleIds 角色ID集合，可为空
     * @param status  角色状态，可为空
     * @return 角色集合
     */
    default List<SysRole> selectListByIdsAndStatus(List<Long> roleIds, Byte status) {
        return selectList(new LambdaQueryWrapperX<SysRole>()
                .inIfPresent(SysRole::getId, roleIds)
                .eqIfPresent(SysRole::getStatus, status)
                .orderByAsc(SysRole::getRoleSort, SysRole::getId));
    }

    /**
     * 判断角色名称是否已存在。
     *
     * @param roleName 角色名称
     * @param excludeId 排除的角色ID，可为空
     * @return true 已存在
     */
    default boolean existsByRoleName(String roleName, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<SysRole>()
                .eq(SysRole::getRoleName, roleName)
                .neIfPresent(SysRole::getId, excludeId)) > 0;
    }

    /**
     * 判断角色编码是否已存在。
     *
     * @param roleCode 角色编码
     * @param excludeId 排除的角色ID，可为空
     * @return true 已存在
     */
    default boolean existsByRoleCode(String roleCode, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .neIfPresent(SysRole::getId, excludeId)) > 0;
    }
}
