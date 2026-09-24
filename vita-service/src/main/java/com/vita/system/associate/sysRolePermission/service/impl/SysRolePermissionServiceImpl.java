package com.vita.system.associate.sysRolePermission.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionCreateDto;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionSearchDto;
import com.vita.system.associate.sysRolePermission.dto.SysRolePermissionUpdateDto;
import com.vita.system.associate.sysRolePermission.entity.SysRolePermission;
import com.vita.system.associate.sysRolePermission.mapper.SysRolePermissionMapper;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionDetailVo;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionListVo;
import com.vita.system.associate.sysRolePermission.vo.SysRolePermissionPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRolePermission.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:40:54
 * @Description: 角色权限关联表 Service 实现
 * @version 1.0
 */
@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission> implements ISysRolePermissionService {

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysRolePermissionCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRolePermission data = new SysRolePermission();
        BeanUtils.copyProperties(createDto, data);
        sysRolePermissionMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysRolePermissionUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysRolePermission data = new SysRolePermission();
        BeanUtils.copyProperties(updateDto, data);
        sysRolePermissionMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysRolePermissionMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysRolePermissionDetailVo get(Long id) {
        SysRolePermission data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysRolePermissionDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysRolePermissionListVo> list(SysRolePermissionSearchDto searchDto) {
        List<SysRolePermission> dataList = sysRolePermissionMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysRolePermissionListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysRolePermissionPageVo> page(SysRolePermissionSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysRolePermissionSearchDto();
        }
        PageResponse<SysRolePermission> selectPage = sysRolePermissionMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysRolePermissionPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysRolePermissionPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 查询角色权限ID集合。
     *
     * @param roleIds 角色ID集合
     * @return 权限ID集合
     */
    @Override
    public List<Long> getPermissionIdsByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        return sysRolePermissionMapper.selectList(new LambdaQueryWrapperX<SysRolePermission>()
                        .inIfPresent(SysRolePermission::getRoleId, roleIds)
                        .orderByAsc(SysRolePermission::getId))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 查询单个角色权限ID集合。
     *
     * @param roleId 角色ID
     * @return 权限ID集合
     */
    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return getPermissionIdsByRoleIds(List.of(roleId));
    }

    /**
     * 根据角色ID列表删除角色权限关联。
     *
     * @param roleIds 角色ID集合
     */
    @Override
    public void removeByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        remove(new LambdaQueryWrapperX<SysRolePermission>().inIfPresent(SysRolePermission::getRoleId, roleIds));
    }

    /**
     * 根据权限ID列表删除角色权限关联。
     *
     * @param permissionIds 权限ID集合
     */
    @Override
    public void removeByPermissionIds(List<Long> permissionIds) {
        if (CollUtil.isEmpty(permissionIds)) {
            return;
        }
        remove(new LambdaQueryWrapperX<SysRolePermission>().inIfPresent(SysRolePermission::getPermissionId, permissionIds));
    }

    /**
     * 根据角色ID和权限ID列表删除角色权限关联。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    @Override
    public void removeByRoleIdAndPermissionIds(Long roleId, List<Long> permissionIds) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        List<Long> validPermissionIds = sanitizeIds(permissionIds);
        if (CollUtil.isEmpty(validPermissionIds)) {
            return;
        }
        remove(new LambdaQueryWrapperX<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId)
                .inIfPresent(SysRolePermission::getPermissionId, validPermissionIds));
    }

    /**
     * 批量为角色分配权限。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    @Override
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        List<Long> validPermissionIds = sanitizeIds(permissionIds);
        if (CollUtil.isEmpty(validPermissionIds)) {
            return;
        }
        List<Long> existingPermissionIds = getPermissionIdsByRoleId(roleId);
        List<SysRolePermission> rolePermissions = validPermissionIds.stream()
                .filter(permissionId -> !existingPermissionIds.contains(permissionId))
                .map(permissionId -> {
                    SysRolePermission rolePermission = new SysRolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermissionId(permissionId);
                    return rolePermission;
                })
                .toList();
        if (CollUtil.isEmpty(rolePermissions)) {
            return;
        }
        saveBatch(rolePermissions);
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysRolePermission> buildQueryWrapper(SysRolePermissionSearchDto searchDto) {
        LambdaQueryWrapperX<SysRolePermission> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysRolePermission::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysRolePermission getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRolePermission data = sysRolePermissionMapper.selectById(id);
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
