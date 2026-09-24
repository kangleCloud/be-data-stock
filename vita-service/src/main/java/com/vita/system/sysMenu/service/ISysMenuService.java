package com.vita.system.sysMenu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.common.core.model.TreeSelectModel;
import com.vita.system.sysMenu.dto.SysMenuCreateDto;
import com.vita.system.sysMenu.dto.SysMenuDeletedDto;
import com.vita.system.sysMenu.dto.SysMenuSearchDto;
import com.vita.system.sysMenu.dto.SysMenuUpdateDto;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.vo.RouterVo;
import com.vita.system.sysMenu.vo.SysMenuDetailVo;
import com.vita.system.sysMenu.vo.SysMenuListVo;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.service
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 Service 接口
 */
public interface ISysMenuService extends IService<SysMenu> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysMenuCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysMenuUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(SysMenuDeletedDto deletedDto);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysMenuDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysMenuListVo> list(SysMenuSearchDto searchDto);


    /**
     * 查询全部启用菜单。
     *
     * @return 菜单集合
     */
    List<SysMenu> getAllEnabledMenus();

    /**
     * 根据菜单ID和状态查询菜单集合。
     *
     * @param menuIds 菜单ID集合
     * @param status  菜单状态，可为空
     * @return 菜单集合
     */
    List<SysMenu> getListByIds(List<Long> menuIds, Byte status);

    /**
     * 根据条件查询菜单原始列表。
     *
     * @param searchDto 查询条件
     * @return 菜单列表
     */
    List<SysMenu> getMenuList(SysMenuSearchDto searchDto);

    /**
     * 根据用户 ID 查询其拥有的菜单原始列表。
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenu> getMenusByUserId(Long userId);

    /**
     * 根据用户 ID 查询其拥有的菜单树。
     *
     * @param userId 用户ID
     * @return 菜单树
     */
    List<SysMenu> getMenuTreeByUserId(Long userId);

    /**
     * 构建菜单下拉树。
     *
     * @param searchDto 查询条件
     * @return 下拉树结构列表
     */
    List<TreeSelectModel> getMenuTreeSelect(SysMenuSearchDto searchDto);

    /**
     * 构建前端所需要下拉树结构。
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    List<TreeSelectModel> buildMenuTreeSelect(List<SysMenu> menus);

    /**
     * 根据用户 ID 获取前端路由树。
     *
     * @param userId 用户ID
     * @return 路由列表
     */
    List<RouterVo> getRoutersByUserId(Long userId);

    /**
     * 构建前端路由所需要的菜单。
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    List<RouterVo> buildRouters(List<SysMenu> menus);

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果 true 唯一 / false 不唯一
     */
    boolean checkMenuNameUnique(SysMenu menu);

    /**
     * 校验路由组合是否唯一
     *
     * @param menu 菜单信息
     * @return 结果 true 唯一 / false 不唯一
     */
    boolean checkRouteConfigUnique(SysMenu menu);
}
