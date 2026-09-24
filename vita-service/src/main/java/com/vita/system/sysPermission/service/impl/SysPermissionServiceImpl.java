package com.vita.system.sysPermission.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.sysPermission.dto.*;
import com.vita.system.sysPermission.entity.SysPermission;
import com.vita.system.sysPermission.mapper.SysPermissionMapper;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysPermission.vo.SysPermissionDetailVo;
import com.vita.system.sysPermission.vo.SysPermissionListVo;
import com.vita.system.sysPermission.vo.SysPermissionOptionVo;
import com.vita.system.sysPermission.vo.SysPermissionPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 Service 实现
 * @version 1.0
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements ISysPermissionService {

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private ISysRolePermissionService sysRolePermissionService;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysPermissionCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysPermission data = new SysPermission();
        BeanUtils.copyProperties(createDto, data);
        normalizeAuthTag(data);
        validatePermissionBeforeSave(data, null);
        sysPermissionMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysPermissionUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysPermission existed = getRequiredEntity(updateDto.getId());
        SysPermission data = new SysPermission();
        BeanUtils.copyProperties(updateDto, data);
        fillMissingPermissionFields(data, existed);
        normalizeAuthTag(data);
        validatePermissionBeforeSave(data, existed);
        sysPermissionMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(SysPermissionDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        doDeletePermissions(CollUtil.newArrayList(deletedDto.getId()));
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysPermissionDetailVo get(Long id) {
        SysPermission data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysPermissionDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysPermissionListVo> list(SysPermissionSearchDto searchDto) {
        List<SysPermission> dataList = sysPermissionMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysPermissionListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysPermissionPageVo> page(SysPermissionSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysPermissionSearchDto();
        }
        PageResponse<SysPermission> selectPage = sysPermissionMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysPermissionPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysPermissionPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 查询全部权限。
     *
     * @return 权限列表
     */
    @Override
    public List<SysPermissionListVo> getAll() {
        return BeanUtil.copyToList(getListByIds(null, null), SysPermissionListVo.class);
    }

    /**
     * 根据权限ID和状态查询权限列表。
     *
     * @param permissionIds 权限ID列表
     * @param status 权限状态，可为空
     * @return 权限列表
     */
    @Override
    public List<SysPermission> getListByIds(List<Long> permissionIds, Byte status) {
        if (permissionIds != null && permissionIds.isEmpty()) {
            return CollUtil.newArrayList();
        }
        List<SysPermission> permissions = sysPermissionMapper.selectListByIdsAndStatus(permissionIds, status);
        if (CollUtil.isEmpty(permissions)) {
            return CollUtil.newArrayList();
        }
        return permissions;
    }

    /**
     * 根据角色ID查询权限列表。
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public List<SysPermissionListVo> getListByRoleId(Long roleId) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        List<Long> permissionIds = sysRolePermissionService.getPermissionIdsByRoleId(roleId);
        if (CollUtil.isEmpty(permissionIds)) {
            return CollUtil.newArrayList();
        }
        return BeanUtil.copyToList(getListByIds(permissionIds, null), SysPermissionListVo.class);
    }

    /**
     * 根据角色ID获取权限选择框列表。
     *
     * @param roleId 角色ID
     * @return 权限选择框列表
     */
    @Override
    public List<SysPermissionOptionVo> getOptionListByRoleId(Long roleId) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        Set<Long> selectedPermissionIds = Set.copyOf(sysRolePermissionService.getPermissionIdsByRoleId(roleId));
        return getListByIds(null, null).stream()
                .map(permission -> {
                    SysPermissionOptionVo optionVo = new SysPermissionOptionVo();
                    optionVo.setId(permission.getId());
                    optionVo.setPermissionName(permission.getPermissionName());
                    optionVo.setPermissionCode(permission.getPermissionCode());
                    optionVo.setAuthTag(permission.getAuthTag());
                    optionVo.setSelected(selectedPermissionIds.contains(permission.getId()));
                    optionVo.setDisabled(isProtectedPermission(permission) || !CommonStatusEnum.isEnabled(permission.getStatus()));
                    return optionVo;
                })
                .toList();
    }

    /**
     * 校验权限编码是否唯一。
     *
     * @param permission 权限信息
     * @return true 唯一 / false 不唯一
     */
    @Override
    public boolean checkPermissionCodeUnique(SysPermission permission) {
        if (permission == null || CharSequenceUtil.isBlank(permission.getPermissionCode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return !sysPermissionMapper.existsByPermissionCode(permission.getPermissionCode(), permission.getId());
    }

    /**
     * 校验鉴权标识是否唯一。
     *
     * @param permission 权限信息
     * @return true 唯一 / false 不唯一
     */
    @Override
    public boolean checkAuthTagUnique(SysPermission permission) {
        if (permission == null || CharSequenceUtil.isBlank(permission.getAuthTag())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return !sysPermissionMapper.existsByAuthTag(permission.getAuthTag(), permission.getId());
    }

    /**
     * 校验权限是否允许操作。
     *
     * @param permissionId 权限ID
     */
    @Override
    public void checkPermissionAllowed(Long permissionId) {
        SysPermission permission = getRequiredEntity(permissionId);
        if (isProtectedPermission(permission)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "系统内置权限不允许操作");
        }
    }

    /**
     * 批量删除权限。
     *
     * @param batchDeletedDto 批量删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(SysPermissionBatchDeletedDto batchDeletedDto) {
        if (batchDeletedDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        doDeletePermissions(batchDeletedDto.getIds());
    }

    /**
     * 统一单删/批删逻辑，避免 public 事务方法同类互调导致事务失效。
     *
     * @param ids 权限ID列表
     */
    private void doDeletePermissions(List<Long> ids) {
        List<Long> validPermissionIds = sanitizeIds(ids);
        if (CollUtil.isEmpty(validPermissionIds)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        validPermissionIds.forEach(this::checkPermissionAllowed);
        sysRolePermissionService.removeByPermissionIds(validPermissionIds);
        validPermissionIds.forEach(sysPermissionMapper::deleteById);
    }

    /**
     * 根据权限ID和状态查询权限码集合。
     *
     * @param permissionIds 权限ID集合
     * @param status 权限状态，可为空
     * @return 权限码集合
     */
    @Override
    public List<String> getPermissionCodesByIds(List<Long> permissionIds, Byte status) {
        if (CollUtil.isEmpty(permissionIds)) {
            return CollUtil.newArrayList();
        }
        return getListByIds(permissionIds, status)
                .stream()
                .map(permission -> StrUtil.isNotBlank(permission.getAuthTag())
                        ? permission.getAuthTag()
                        : permission.getPermissionCode())
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysPermission> buildQueryWrapper(SysPermissionSearchDto searchDto) {
        LambdaQueryWrapperX<SysPermission> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null) {
            queryWrapper.likeIfPresent(SysPermission::getPermissionName, searchDto.getPermissionName())
                    .likeIfPresent(SysPermission::getPermissionCode, searchDto.getPermissionCode())
                    .eqIfPresent(SysPermission::getPermissionType, searchDto.getPermissionType())
                    .eqIfPresent(SysPermission::getMenuId, searchDto.getMenuId())
                    .eqIfPresent(SysPermission::getStatus, searchDto.getStatus());
            if (StrUtil.isNotBlank(searchDto.getKeyword())) {
                queryWrapper.and(wrapper -> wrapper
                        .like(SysPermission::getPermissionName, searchDto.getKeyword())
                        .or()
                        .like(SysPermission::getPermissionCode, searchDto.getKeyword())
                        .or()
                        .like(SysPermission::getAuthTag, searchDto.getKeyword()));
            }
        }
        queryWrapper.orderByAsc(SysPermission::getSortNo, SysPermission::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysPermission getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysPermission data = sysPermissionMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }

    private void validatePermissionBeforeSave(SysPermission target, SysPermission existed) {
        if (target == null
                || CharSequenceUtil.isBlank(target.getPermissionName())
                || CharSequenceUtil.isBlank(target.getPermissionCode())
                || CharSequenceUtil.isBlank(target.getPermissionType())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        if (!checkPermissionCodeUnique(target)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "权限编码已存在");
        }
        if (!checkAuthTagUnique(target)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "鉴权标识已存在");
        }
        if (existed != null && isProtectedPermission(existed)) {
            validateProtectedPermissionUpdate(existed, target);
        }
    }

    private void fillMissingPermissionFields(SysPermission target, SysPermission existed) {
        if (CharSequenceUtil.isBlank(target.getPermissionName())) {
            target.setPermissionName(existed.getPermissionName());
        }
        if (CharSequenceUtil.isBlank(target.getPermissionCode())) {
            target.setPermissionCode(existed.getPermissionCode());
        }
        if (CharSequenceUtil.isBlank(target.getPermissionType())) {
            target.setPermissionType(existed.getPermissionType());
        }
        if (target.getMenuId() == null) {
            target.setMenuId(existed.getMenuId());
        }
        if (CharSequenceUtil.isBlank(target.getApiMethod())) {
            target.setApiMethod(existed.getApiMethod());
        }
        if (CharSequenceUtil.isBlank(target.getApiPath())) {
            target.setApiPath(existed.getApiPath());
        }
        if (target.getSortNo() == null) {
            target.setSortNo(existed.getSortNo());
        }
        if (target.getStatus() == null) {
            target.setStatus(existed.getStatus());
        }
        if (target.getIsSystem() == null) {
            target.setIsSystem(existed.getIsSystem());
        }
        if (target.getRemark() == null) {
            target.setRemark(existed.getRemark());
        }
    }

    private void normalizeAuthTag(SysPermission permission) {
        if (permission == null) {
            return;
        }
        if (StrUtil.isBlank(permission.getAuthTag())) {
            permission.setAuthTag(permission.getPermissionCode());
        }
    }

    private void validateProtectedPermissionUpdate(SysPermission existed, SysPermission target) {
        if (isProtectedPermission(existed)
                && (!Objects.equals(existed.getPermissionCode(), target.getPermissionCode())
                || !Objects.equals(existed.getAuthTag(), target.getAuthTag()))) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "系统内置权限不允许修改权限编码或鉴权标识");
        }
    }

    private boolean isProtectedPermission(SysPermission permission) {
        return CommonStatusEnum.isEnabled(permission.getIsSystem());
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
