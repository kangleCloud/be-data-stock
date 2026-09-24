package com.vita.system.associate.sysUserRole.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleCreateDto;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleSearchDto;
import com.vita.system.associate.sysUserRole.dto.SysUserRoleUpdateDto;
import com.vita.system.associate.sysUserRole.entity.SysUserRole;
import com.vita.system.associate.sysUserRole.mapper.SysUserRoleMapper;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.associate.sysUserRole.vo.SysUserRoleDetailVo;
import com.vita.system.associate.sysUserRole.vo.SysUserRoleListVo;
import com.vita.system.associate.sysUserRole.vo.SysUserRolePageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysUserRole.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:39:20
 * @Description: 用户角色关联表 Service 实现
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements ISysUserRoleService {

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysUserRoleCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysUserRole data = new SysUserRole();
        BeanUtils.copyProperties(createDto, data);
        sysUserRoleMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysUserRoleUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysUserRole data = new SysUserRole();
        BeanUtils.copyProperties(updateDto, data);
        sysUserRoleMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysUserRoleMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysUserRoleDetailVo get(Long id) {
        SysUserRole data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysUserRoleDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysUserRoleListVo> list(SysUserRoleSearchDto searchDto) {
        List<SysUserRole> dataList = sysUserRoleMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysUserRoleListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysUserRolePageVo> page(SysUserRoleSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysUserRoleSearchDto();
        }
        PageResponse<SysUserRole> selectPage = sysUserRoleMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysUserRolePageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysUserRolePageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 根据用户ID获取其拥有的角色ID列表。
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return sysUserRoleMapper.selectRoleIdsByUserId(userId);
    }

    /**
     * 根据角色ID获取其拥有的用户ID列表。
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    @Override
    public List<Long> getUserIdsByRoleId(Long roleId) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return sysUserRoleMapper.selectUserIdsByRoleId(roleId);
    }

    /**
     * 根据角色ID列表删除用户角色关联。
     *
     * @param roleIds 角色ID列表
     */
    @Override
    public void removeByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        remove(new LambdaQueryWrapperX<SysUserRole>().inIfPresent(SysUserRole::getRoleId, roleIds));
    }

    /**
     * 根据角色ID和用户ID列表删除用户角色关联。
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    @Override
    public void removeByRoleIdAndUserIds(Long roleId, List<Long> userIds) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        List<Long> validUserIds = sanitizeIds(userIds);
        if (CollUtil.isEmpty(validUserIds)) {
            return;
        }
        remove(new LambdaQueryWrapperX<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId)
                .inIfPresent(SysUserRole::getUserId, validUserIds));
    }

    /**
     * 批量为用户分配角色。
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    @Override
    public void assignRoleToUsers(Long roleId, List<Long> userIds) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        List<Long> validUserIds = sanitizeIds(userIds);
        if (CollUtil.isEmpty(validUserIds)) {
            return;
        }
        List<Long> existingUserIds = getUserIdsByRoleId(roleId);
        List<SysUserRole> userRoles = validUserIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .map(userId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setRoleId(roleId);
                    userRole.setUserId(userId);
                    return userRole;
                })
                .toList();
        if (CollUtil.isEmpty(userRoles)) {
            return;
        }
        saveBatch(userRoles);
    }

    /**
     * 构造通用查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysUserRole> buildQueryWrapper(SysUserRoleSearchDto searchDto) {
        LambdaQueryWrapperX<SysUserRole> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null) {
            queryWrapper.eqIfPresent(SysUserRole::getUserId, searchDto.getUserId());
        }
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysUserRole getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysUserRole data = sysUserRoleMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
