package com.vita.system.associate.sysRoleMenu.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuCreateDto;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuSearchDto;
import com.vita.system.associate.sysRoleMenu.dto.SysRoleMenuUpdateDto;
import com.vita.system.associate.sysRoleMenu.entity.SysRoleMenu;
import com.vita.system.associate.sysRoleMenu.mapper.SysRoleMenuMapper;
import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuDetailVo;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuListVo;
import com.vita.system.associate.sysRoleMenu.vo.SysRoleMenuPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleMenu.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:40:21
 * @Description: 角色菜单关联表 Service 实现
 */
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements ISysRoleMenuService {

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysRoleMenuCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRoleMenu data = new SysRoleMenu();
        BeanUtils.copyProperties(createDto, data);
        sysRoleMenuMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysRoleMenuUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysRoleMenu data = new SysRoleMenu();
        BeanUtils.copyProperties(updateDto, data);
        sysRoleMenuMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysRoleMenuMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysRoleMenuDetailVo get(Long id) {
        SysRoleMenu data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysRoleMenuDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysRoleMenuListVo> list(SysRoleMenuSearchDto searchDto) {
        List<SysRoleMenu> dataList = sysRoleMenuMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysRoleMenuListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysRoleMenuPageVo> page(SysRoleMenuSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysRoleMenuSearchDto();
        }
        PageResponse<SysRoleMenu> selectPage = sysRoleMenuMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysRoleMenuPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysRoleMenuPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 查询角色菜单ID集合。
     *
     * @param roleIds 角色ID集合
     * @return 菜单ID集合
     */
    @Override
    public List<Long> getMenuIdsByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        List<Long> menuIds = sysRoleMenuMapper.selectMenuIdsByRoleIds(roleIds);
        if (CollUtil.isEmpty(menuIds)) {
            return CollUtil.newArrayList();
        }
        return menuIds;
    }

    /**
     * 根据角色ID列表删除角色菜单关联。
     *
     * @param roleIds 角色ID集合
     */
    @Override
    public void removeByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        remove(new LambdaQueryWrapperX<SysRoleMenu>().inIfPresent(SysRoleMenu::getRoleId, roleIds));
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysRoleMenu> buildQueryWrapper(SysRoleMenuSearchDto searchDto) {
        LambdaQueryWrapperX<SysRoleMenu> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysRoleMenu::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysRoleMenu getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRoleMenu data = sysRoleMenuMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
