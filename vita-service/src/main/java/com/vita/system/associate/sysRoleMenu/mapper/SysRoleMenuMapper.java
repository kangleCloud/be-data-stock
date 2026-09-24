package com.vita.system.associate.sysRoleMenu.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.system.associate.sysRoleMenu.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleMenu.mapper
 * @Author znk
 * @CreateTime 2026-03-31 16:40:21
 * @Description: 角色菜单关联表 Mapper 接口
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapperX<SysRoleMenu> {

    /**
     * 查询角色菜单ID集合。
     *
     * @param roleIds 角色ID集合
     * @return 菜单ID集合
     */
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
