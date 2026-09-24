package com.vita.system.sysMenu.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.common.core.model.TreeSelectModel;
import com.vita.core.constant.Constants;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysMenu.dto.SysMenuCreateDto;
import com.vita.system.sysMenu.dto.SysMenuDeletedDto;
import com.vita.system.sysMenu.dto.SysMenuSearchDto;
import com.vita.system.sysMenu.dto.SysMenuUpdateDto;
import com.vita.system.sysMenu.entity.SysMenu;
import com.vita.system.sysMenu.mapper.SysMenuMapper;
import com.vita.system.sysMenu.service.ISysMenuService;
import com.vita.system.sysMenu.vo.MetaVo;
import com.vita.system.sysMenu.vo.RouterVo;
import com.vita.system.sysMenu.vo.SysMenuDetailVo;
import com.vita.system.sysMenu.vo.SysMenuListVo;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 Service 实现
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    private static final Long ROOT_MENU_ID = 0L;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private ISysUserService sysUserService;

    @Resource
    private ISysUserRoleService sysUserRoleService;

    @Resource
    private ISysRoleMenuService sysRoleMenuService;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysMenuCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysMenu data = new SysMenu();
        BeanUtils.copyProperties(createDto, data);
        validateMenuUnique(data);
        sysMenuMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysMenuUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysMenu data = new SysMenu();
        BeanUtils.copyProperties(updateDto, data);
        validateMenuUnique(data);
        sysMenuMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(SysMenuDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        Long id = deletedDto.getId();
        getRequiredEntity(id);
        if (!hasChildNode(id)) {
            sysMenuMapper.deleteById(id);
        } else {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "存在子菜单，无法删除");
        }
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysMenuDetailVo get(Long id) {
        SysMenu data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysMenuDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysMenuListVo> list(SysMenuSearchDto searchDto) {
        List<SysMenu> dataList = getMenuList(searchDto);
        return BeanUtil.copyToList(dataList, SysMenuListVo.class);
    }


    /**
     * 查询全部启用菜单。
     *
     * @return 菜单集合
     */
    @Override
    public List<SysMenu> getAllEnabledMenus() {
        List<SysMenu> menuList = sysMenuMapper.selectAllEnabledMenus();
        if (CollUtil.isEmpty(menuList)) {
            return CollUtil.newArrayList();
        }
        return menuList;
    }

    /**
     * 根据菜单ID和状态查询菜单集合。
     *
     * @param menuIds 菜单ID集合
     * @param status  菜单状态，可为空
     * @return 菜单集合
     */
    @Override
    public List<SysMenu> getListByIds(List<Long> menuIds, Byte status) {
        if (CollUtil.isEmpty(menuIds)) {
            return CollUtil.newArrayList();
        }
        List<SysMenu> menuList = sysMenuMapper.selectList(new LambdaQueryWrapperX<SysMenu>()
                .inIfPresent(SysMenu::getId, menuIds)
                .eqIfPresent(SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getSortNo, SysMenu::getId));
        if (CollUtil.isEmpty(menuList)) {
            return CollUtil.newArrayList();
        }
        return menuList;
    }

    @Override
    public List<SysMenu> getMenuList(SysMenuSearchDto searchDto) {
        List<SysMenu> menuList = sysMenuMapper.selectList(buildQueryWrapper(searchDto));
        if (CollUtil.isEmpty(menuList)) {
            return CollUtil.newArrayList();
        }
        return menuList;
    }

    @Override
    public List<SysMenu> getMenusByUserId(Long userId) {
        SysUser user = sysUserService.getActiveById(userId);
        if (user == null) {
            return CollUtil.newArrayList();
        }
        if (CommonStatusEnum.isEnabled(user.getIsSuperAdmin())) {
            return getAllEnabledMenus();
        }
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        List<Long> menuIds = sysRoleMenuService.getMenuIdsByRoleIds(roleIds);
        if (CollUtil.isEmpty(menuIds)) {
            return CollUtil.newArrayList();
        }
        return getListByIds(menuIds, CommonStatusEnum.ENABLED.getCode());
    }

    /**
     * 根据用户 ID 查询其拥有的菜单树。
     *
     * @param userId 用户ID
     * @return 菜单树
     */
    @Override
    public List<SysMenu> getMenuTreeByUserId(Long userId) {
        return buildMenuTree(getMenusByUserId(userId));
    }

    /**
     * 构建菜单下拉树。
     *
     * @param searchDto 查询条件
     * @return 下拉树
     */
    @Override
    public List<TreeSelectModel> getMenuTreeSelect(SysMenuSearchDto searchDto) {
        return buildMenuTreeSelect(getMenuList(searchDto));
    }

    /**
     * 构建前端所需要下拉树结构。
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelectModel> buildMenuTreeSelect(List<SysMenu> menus) {
        List<SysMenu> menuTree = buildMenuTree(menus);
        if (CollUtil.isEmpty(menuTree)) {
            return CollUtil.newArrayList();
        }
        return menuTree.stream()
                .map(TreeSelectModel::new)
                .toList();
    }

    /**
     * 根据用户 ID 获取前端路由树。
     *
     * @param userId 用户 ID
     * @return 路由树
     */
    @Override
    public List<RouterVo> getRoutersByUserId(Long userId) {
        return buildRouters(getMenusByUserId(userId));
    }

    /**
     * 构建前端路由所需要的菜单。
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildRouters(List<SysMenu> menus) {
        List<SysMenu> menuTree = buildMenuTree(menus);
        return buildRouterChildren(menuTree);
    }

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果 true 唯一 / false 不唯一
     */
    @Override
    public boolean checkMenuNameUnique(SysMenu menu) {
        if (menu == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        Long count = sysMenuMapper.selectCount(new LambdaQueryWrapperX<SysMenu>()
                .eq(SysMenu::getMenuName, menu.getMenuName())
                .eq(SysMenu::getParentId, menu.getParentId())
                .neIfPresent(SysMenu::getId, menu.getId()));
        if (count != null && count > 0) {
            return Constants.NOT_UNIQUE;
        }
        return Constants.UNIQUE;
    }

    /**
     * 校验路由组合是否唯一
     *
     * @param menu 菜单信息
     * @return 结果 true 唯一 / false 不唯一
     */
    @Override
    public boolean checkRouteConfigUnique(SysMenu menu) {
        if (menu == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        boolean hasRouteName = CharSequenceUtil.isNotBlank(menu.getRouteName());
        boolean hasRouteLink = CharSequenceUtil.isNotBlank(menu.getRouteLink());
        if (!hasRouteName && !hasRouteLink) {
            return Constants.UNIQUE;
        }
        if (hasRouteName) {
            Long routeNameCount = sysMenuMapper.selectCount(new LambdaQueryWrapperX<SysMenu>()
                    .eq(SysMenu::getRouteName, menu.getRouteName())
                    .neIfPresent(SysMenu::getId, menu.getId()));
            if (routeNameCount != null && routeNameCount > 0) {
                return Constants.NOT_UNIQUE;
            }
        }
        if (hasRouteLink) {
            Long routeLinkCount = sysMenuMapper.selectCount(new LambdaQueryWrapperX<SysMenu>()
                    .eq(SysMenu::getRouteLink, menu.getRouteLink())
                    .neIfPresent(SysMenu::getId, menu.getId()));
            if (routeLinkCount != null && routeLinkCount > 0) {
                return Constants.NOT_UNIQUE;
            }
        }
        return Constants.UNIQUE;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysMenu> buildQueryWrapper(SysMenuSearchDto searchDto) {
        LambdaQueryWrapperX<SysMenu> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null) {
            queryWrapper.likeIfPresent(SysMenu::getMenuName, searchDto.getMenuName())
                    .eqIfPresent(SysMenu::getMenuType, searchDto.getMenuType())
                    .eqIfPresent(SysMenu::getStatus, searchDto.getStatus());
        }
        queryWrapper.orderByAsc(SysMenu::getSortNo, SysMenu::getId);
        return queryWrapper;
    }

    /**
     * 校验菜单名称和路由配置唯一性。
     *
     * @param menu 菜单信息
     */
    private void validateMenuUnique(SysMenu menu) {
        if (!checkMenuNameUnique(menu)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "同级菜单名称已存在");
        }
        if (!checkRouteConfigUnique(menu)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "路由名称或路由地址已存在");
        }
        if (menu.getIsExternal() != null && menu.getIsExternal() == 1) {
            if (CharSequenceUtil.isBlank(menu.getRouteLink())) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "外链菜单必须填写路由地址");
            }
            if (CharSequenceUtil.isNotBlank(menu.getRouteLink())
                    && !CharSequenceUtil.startWithAny(menu.getRouteLink(), "http://", "https://")) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "外链菜单的路由地址必须以 http:// 或 https:// 开头");
            }
        }
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysMenu getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysMenu data = sysMenuMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }

    /**
     * 将平铺菜单列表组装为树结构。
     *
     * @param menus 菜单集合
     * @return 菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }
        List<SysMenu> menuList = new ArrayList<>(menus);
        menuList.forEach(menu -> menu.setChildren(new ArrayList<>()));
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu menu : menuList) {
            if (isRootMenu(menu)) {
                roots.add(menu);
                continue;
            }
            SysMenu parentMenu = findMenuById(menuList, menu.getParentId());
            if (parentMenu == null) {
                roots.add(menu);
                continue;
            }
            parentMenu.getChildren().add(menu);
        }
        sortMenuTree(roots);
        return roots;
    }

    /**
     * 递归构建路由树。
     *
     * @param menuTree 菜单树
     * @return 路由集合
     */
    private List<RouterVo> buildRouterChildren(List<SysMenu> menuTree) {
        List<RouterVo> routers = new ArrayList<>();
        for (SysMenu menu : menuTree) {
            RouterVo routerVo = new RouterVo();
            routerVo.setRouteName(menu.getRouteName());
            routerVo.setRouteLink(menu.getRouteLink());
            routerVo.setPath(menu.getRouteLink());
            routerVo.setHidden(CommonStatusEnum.isDisabled(menu.getVisible()));
            routerVo.setComponent(menu.getComponentPath());
            routerVo.setAlwaysShow(CommonStatusEnum.isEnabled(menu.getAlwaysShow()));
            routerVo.setMeta(buildMeta(menu));
            List<RouterVo> children = buildRouterChildren(menu.getChildren());
            if (CollUtil.isNotEmpty(children)) {
                routerVo.setChildren(children);
            }
            routers.add(routerVo);
        }
        return routers;
    }

    /**
     * 递归排序菜单树。
     *
     * @param menus 菜单树
     */
    private void sortMenuTree(List<SysMenu> menus) {
        menus.sort((left, right) -> {
            int sortCompare = Integer.compare(left.getSortNo() == null ? Integer.MAX_VALUE : left.getSortNo(),
                    right.getSortNo() == null ? Integer.MAX_VALUE : right.getSortNo());
            if (sortCompare != 0) {
                return sortCompare;
            }
            long leftId = left.getId() == null ? Long.MAX_VALUE : left.getId();
            long rightId = right.getId() == null ? Long.MAX_VALUE : right.getId();
            return Long.compare(leftId, rightId);
        });
        for (SysMenu menu : menus) {
            if (CollUtil.isNotEmpty(menu.getChildren())) {
                sortMenuTree(menu.getChildren());
            }
        }
    }

    /**
     * 判断菜单是否为根节点。
     *
     * @param menu 菜单
     * @return true 根节点 / false 非根节点
     */
    private boolean isRootMenu(SysMenu menu) {
        return menu == null || menu.getParentId() == null || Objects.equals(ROOT_MENU_ID, menu.getParentId());
    }

    /**
     * 按菜单 ID 查找节点。
     *
     * @param menus  菜单集合
     * @param menuId 菜单 ID
     * @return 菜单节点
     */
    private SysMenu findMenuById(List<SysMenu> menus, Long menuId) {
        if (menuId == null) {
            return null;
        }
        for (SysMenu menu : menus) {
            if (Objects.equals(menuId, menu.getId())) {
                return menu;
            }
        }
        return null;
    }

    /**
     * 构建路由元信息。
     *
     * @param menu 菜单
     * @return 路由元信息
     */
    private MetaVo buildMeta(SysMenu menu) {
        MetaVo metaVo = new MetaVo();
        metaVo.setMenuName(menu.getMenuName());
        metaVo.setIcon(menu.getIcon());
        metaVo.setIsCache(menu.getIsCache());
        return metaVo;
    }

    /**
     * 判断是否拥有子节点。
     *
     * @param menuId 菜单ID
     * @return 结果 true 存在 / false 不存在
     */
    private boolean hasChildNode(Long menuId) {
        Long count = sysMenuMapper.selectCount(new LambdaQueryWrapperX<SysMenu>()
                .eq(SysMenu::getParentId, menuId));
        return count != null && count > 0;
    }
}
