package com.vita.system.sysMenu.mapper;

import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.system.sysMenu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.mapper
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface SysMenuMapper extends BaseMapperX<SysMenu> {

    /**
     * 查询全部启用菜单。
     *
     * @return 菜单集合
     */
    List<SysMenu> selectAllEnabledMenus();
}
