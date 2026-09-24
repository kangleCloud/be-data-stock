package com.vita.system.sysRole.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.auth.constant.AuthConstants;
import com.vita.core.constant.Constants;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.associate.sysRoleDeptScope.service.ISysRoleDeptScopeService;
import com.vita.system.associate.sysRoleMenu.service.ISysRoleMenuService;
import com.vita.system.associate.sysRolePermission.service.ISysRolePermissionService;
import com.vita.system.associate.sysUserRole.service.ISysUserRoleService;
import com.vita.system.sysPermission.entity.SysPermission;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysRole.dto.*;
import com.vita.system.sysRole.entity.SysRole;
import com.vita.system.sysRole.mapper.SysRoleMapper;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysRole.vo.SysRoleDetailVo;
import com.vita.system.sysRole.vo.SysRoleListVo;
import com.vita.system.sysRole.vo.SysRoleOptionVo;
import com.vita.system.sysRole.vo.SysRolePageVo;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 Service 实现
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private ISysUserRoleService sysUserRoleService;

    @Resource
    private ISysRoleMenuService sysRoleMenuService;

    @Resource
    private ISysRolePermissionService sysRolePermissionService;

    @Resource
    private ISysRoleDeptScopeService sysRoleDeptScopeService;

    @Resource
    private ISysUserService sysUserService;

    @Resource
    private ISysPermissionService sysPermissionService;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysRoleCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRole data = new SysRole();
        BeanUtils.copyProperties(createDto, data);
        validateRoleBeforeSave(data);
        sysRoleMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysRoleUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRole existed = getRequiredEntity(updateDto.getId());
        checkRoleAllowed(existed.getId());
        SysRole data = new SysRole();
        BeanUtils.copyProperties(updateDto, data);
        fillMissingRoleFields(data, existed);
        validateRoleBeforeSave(data);
        sysRoleMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(SysRoleDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        doDeleteRoles(CollUtil.newArrayList(deletedDto.getId()));
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysRoleDetailVo get(Long id) {
        SysRole data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysRoleDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysRoleListVo> list(SysRoleSearchDto searchDto) {
        List<SysRole> dataList = sysRoleMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysRoleListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysRolePageVo> page(SysRoleSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysRoleSearchDto();
        }
        PageResponse<SysRole> selectPage = sysRoleMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysRolePageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysRolePageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 根据角色ID和状态列表查询角色信息。
     *
     * @param roleIds 角色ID列表
     * @param status  角色状态（可选）
     * @return 角色信息列表
     */
    @Override
    public List<SysRole> getListByIds(List<Long> roleIds, Byte status) {
        if (roleIds != null && roleIds.isEmpty()) {
            return CollUtil.newArrayList();
        }
        List<SysRole> sysRoleList = sysRoleMapper.selectListByIdsAndStatus(roleIds, status);
        if (CollUtil.isNotEmpty(sysRoleList)) {
            return sysRoleList;
        }
        return CollUtil.newArrayList();
    }

    /**
     * 根据角色ID和状态列表查询角色编码。
     *
     * @param roleIds 角色ID列表
     * @param status  角色状态（可选）
     * @return 角色编码列表
     */
    @Override
    public List<String> getRoleCodesByIds(List<Long> roleIds, Byte status) {
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        List<String> roleCodes = sysRoleMapper.selectRoleCodesByIds(roleIds, status);
        if (CollUtil.isEmpty(roleCodes)) {
            return CollUtil.newArrayList();
        }
        return roleCodes.stream()
                .filter(CharSequenceUtil::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 根据用户ID查询角色列表。
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRoleListVo> getListByUserId(Long userId) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return CollUtil.newArrayList();
        }
        return BeanUtil.copyToList(getListByIds(roleIds, null), SysRoleListVo.class);
    }

    /**
     * 查询全部角色。
     *
     * @return 角色列表
     */
    @Override
    public List<SysRoleListVo> getAll() {
        return BeanUtil.copyToList(getListByIds(null, null), SysRoleListVo.class);
    }

    /**
     * 根据用户ID获取角色选择框列表。
     *
     * @param userId 用户ID
     * @return 角色选择框列表
     */
    @Override
    public List<SysRoleOptionVo> getOptionListByUserId(Long userId) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        Set<Long> selectedRoleIds = Set.copyOf(sysUserRoleService.getRoleIdsByUserId(userId));
        return getListByIds(null, null).stream()
                .map(role -> {
                    SysRoleOptionVo optionVo = new SysRoleOptionVo();
                    optionVo.setId(role.getId());
                    optionVo.setRoleName(role.getRoleName());
                    optionVo.setRoleCode(role.getRoleCode());
                    optionVo.setSelected(selectedRoleIds.contains(role.getId()));
                    optionVo.setDisabled(isProtectedRole(role) || !CommonStatusEnum.isEnabled(role.getStatus()));
                    return optionVo;
                })
                .toList();
    }

    /**
     * 校验角色名称是否唯一。
     *
     * @param role 角色信息
     * @return true 唯一 / false 不唯一
     */
    @Override
    public boolean checkRoleNameUnique(SysRole role) {
        if (role == null || CharSequenceUtil.isBlank(role.getRoleName())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return !sysRoleMapper.existsByRoleName(role.getRoleName(), role.getId());
    }

    /**
     * 校验角色编码是否唯一。
     *
     * @param role 角色信息
     * @return true 唯一 / false 不唯一
     */
    @Override
    public boolean checkRoleCodeUnique(SysRole role) {
        if (role == null || CharSequenceUtil.isBlank(role.getRoleCode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        if (AuthConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            return Constants.NOT_UNIQUE;
        }
        return !sysRoleMapper.existsByRoleCode(role.getRoleCode(), role.getId());
    }

    /**
     * 校验角色是否允许操作。
     *
     * @param roleId 角色ID
     */
    @Override
    public void checkRoleAllowed(Long roleId) {
        SysRole role = getRequiredEntity(roleId);
        if (isProtectedRole(role)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "系统内置角色不允许操作");
        }
    }

    /**
     * 批量删除角色。
     *
     * @param batchDeletedDto 批量删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(SysRoleBatchDeletedDto batchDeletedDto) {
        if (batchDeletedDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        doDeleteRoles(batchDeletedDto.getIds());
    }

    /**
     * 统一单删/批删逻辑，避免 public 事务方法同类互调导致事务失效。
     *
     * @param ids 角色ID列表
     */
    private void doDeleteRoles(List<Long> ids) {
        List<Long> validRoleIds = sanitizeIds(ids);
        if (CollUtil.isEmpty(validRoleIds)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        validRoleIds.forEach(this::checkRoleAllowed);
        sysUserRoleService.removeByRoleIds(validRoleIds);
        sysRoleMenuService.removeByRoleIds(validRoleIds);
        sysRolePermissionService.removeByRoleIds(validRoleIds);
        sysRoleDeptScopeService.removeByRoleIds(validRoleIds);
        validRoleIds.forEach(sysRoleMapper::deleteById);
    }

    /**
     * 取消授权单个用户角色。
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAuthUser(Long roleId, Long userId) {
        if (roleId == null || userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        checkRoleAllowed(roleId);
        sysUserRoleService.removeByRoleIdAndUserIds(roleId, List.of(userId));
    }

    /**
     * 批量取消授权用户角色。
     *
     * @param roleId  角色ID
     * @param userIds 用户ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAuthUsers(Long roleId, List<Long> userIds) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        checkRoleAllowed(roleId);
        sysUserRoleService.removeByRoleIdAndUserIds(roleId, userIds);
    }

    /**
     * 批量选择授权用户角色。
     *
     * @param roleId  角色ID
     * @param userIds 用户ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authUsers(Long roleId, List<Long> userIds) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        List<Long> validUserIds = sanitizeIds(userIds);
        if (CollUtil.isEmpty(validUserIds)) {
            return;
        }
        checkRoleAllowed(roleId);
        validateUserIdsExist(validUserIds);
        sysUserRoleService.assignRoleToUsers(roleId, validUserIds);
    }

    /**
     * 取消授权单个角色权限。
     *
     * @param roleId 角色ID
     * @param permissionId 权限ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAuthPermission(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        checkRoleAllowed(roleId);
        sysRolePermissionService.removeByRoleIdAndPermissionIds(roleId, List.of(permissionId));
    }

    /**
     * 批量取消授权角色权限。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAuthPermissions(Long roleId, List<Long> permissionIds) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        checkRoleAllowed(roleId);
        sysRolePermissionService.removeByRoleIdAndPermissionIds(roleId, permissionIds);
    }

    /**
     * 批量选择授权角色权限。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authPermissions(Long roleId, List<Long> permissionIds) {
        if (roleId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        List<Long> validPermissionIds = sanitizeIds(permissionIds);
        if (CollUtil.isEmpty(validPermissionIds)) {
            return;
        }
        checkRoleAllowed(roleId);
        validatePermissionIdsExist(validPermissionIds);
        sysRolePermissionService.assignPermissionsToRole(roleId, validPermissionIds);
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysRole> buildQueryWrapper(SysRoleSearchDto searchDto) {
        LambdaQueryWrapperX<SysRole> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null) {
            queryWrapper.eqIfPresent(SysRole::getId, searchDto.getId())
                    .likeIfPresent(SysRole::getRoleName, searchDto.getRoleName())
                    .likeIfPresent(SysRole::getRoleCode, searchDto.getRoleCode())
                    .eqIfPresent(SysRole::getStatus, searchDto.getStatus());
        }
        queryWrapper.orderByAsc(SysRole::getRoleSort, SysRole::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysRole getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRole data = sysRoleMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }

    private void validateRoleBeforeSave(SysRole role) {
        if (role == null || CharSequenceUtil.isBlank(role.getRoleName()) || CharSequenceUtil.isBlank(role.getRoleCode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        if (AuthConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "角色编码 SUPER_ADMIN 为系统保留值");
        }
        if (!checkRoleNameUnique(role)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "角色名称已存在");
        }
        if (!checkRoleCodeUnique(role)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "角色编码已存在");
        }
    }

    private void fillMissingRoleFields(SysRole target, SysRole existed) {
        if (CharSequenceUtil.isBlank(target.getRoleName())) {
            target.setRoleName(existed.getRoleName());
        }
        if (CharSequenceUtil.isBlank(target.getRoleCode())) {
            target.setRoleCode(existed.getRoleCode());
        }
        if (target.getRoleSort() == null) {
            target.setRoleSort(existed.getRoleSort());
        }
        if (target.getStatus() == null) {
            target.setStatus(existed.getStatus());
        }
        if (CharSequenceUtil.isBlank(target.getDataScopeType())) {
            target.setDataScopeType(existed.getDataScopeType());
        }
        if (target.getIsSystem() == null) {
            target.setIsSystem(existed.getIsSystem());
        }
    }

    private boolean isProtectedRole(SysRole role) {
        return CommonStatusEnum.isEnabled(role.getIsSystem())
                || AuthConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode());
    }


    private List<Long> sanitizeIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return CollUtil.newArrayList();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private void validateUserIdsExist(List<Long> userIds) {
        List<SysUser> users = sysUserService.listByIds(userIds);
        if (CollUtil.isEmpty(users) || users.size() != userIds.size()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "存在无效的用户ID");
        }
    }

    private void validatePermissionIdsExist(List<Long> permissionIds) {
        List<SysPermission> permissions = sysPermissionService.listByIds(permissionIds);
        if (CollUtil.isEmpty(permissions) || permissions.size() != permissionIds.size()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "存在无效的权限ID");
        }
    }
}
