package com.vita.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.system.sysRole.dto.*;
import com.vita.system.sysRole.entity.SysRole;
import com.vita.system.sysRole.service.ISysRoleService;
import com.vita.system.sysRole.vo.SysRoleDetailVo;
import com.vita.system.sysRole.vo.SysRoleListVo;
import com.vita.system.sysRole.vo.SysRoleOptionVo;
import com.vita.system.sysRole.vo.SysRolePageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysRole")
public class SysRoleController extends BaseController {

    /**
     * 角色表 服务。
     */
    private final ISysRoleService sysRoleService;

    public SysRoleController(ISysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @RepeatSubmit
    @PostMapping("/add")
    @SaCheckPermission("system:role:add")
    public CommonResult<Long> create(@RequestBody @Valid SysRoleCreateDto createDto) {
        return CommonResult.success(sysRoleService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @RepeatSubmit
    @PostMapping("/update")
    @SaCheckPermission("system:role:update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysRoleUpdateDto updateDto) {
        sysRoleService.update(updateDto);
        return CommonResult.success(true);
    }

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     * @return 删除结果
     */
    @RepeatSubmit
    @PostMapping("/delete")
    @SaCheckPermission("system:role:delete")
    public CommonResult<Boolean> delete(@RequestBody @Valid SysRoleDeletedDto deletedDto) {
        sysRoleService.delete(deletedDto);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    @SaCheckPermission("system:role:view")
    public CommonResult<SysRoleDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysRoleService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    @SaCheckPermission("system:role:view")
    public CommonResult<List<SysRoleListVo>> list(@Valid SysRoleSearchDto searchDto) {
        return CommonResult.success(sysRoleService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    @SaCheckPermission("system:role:view")
    public CommonResult<PageResponse<SysRolePageVo>> page(@Valid SysRoleSearchDto searchDto) {
        return CommonResult.success(sysRoleService.page(searchDto));
    }

    /**
     * 根据用户ID查询角色。
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @GetMapping("/getByUserId")
    @SaCheckPermission("system:role:view")
    public CommonResult<List<SysRoleListVo>> getByUserId(@RequestParam("userId") Long userId) {
        return CommonResult.success(sysRoleService.getListByUserId(userId));
    }

    /**
     * 查询所有角色。
     *
     * @return 角色列表
     */
    @GetMapping("/getAll")
    @SaCheckPermission("system:role:view")
    public CommonResult<List<SysRoleListVo>> getAll() {
        return CommonResult.success(sysRoleService.getAll());
    }

    /**
     * 根据用户ID获取角色选择框列表。
     *
     * @param userId 用户ID
     * @return 角色选择框列表
     */
    @GetMapping("/getOptionListByUserId")
    @SaCheckPermission("system:role:view")
    public CommonResult<List<SysRoleOptionVo>> getOptionListByUserId(@RequestParam("userId") Long userId) {
        return CommonResult.success(sysRoleService.getOptionListByUserId(userId));
    }

    /**
     * 校验角色名称是否唯一。
     *
     * @param id 角色ID，可为空
     * @param roleName 角色名称
     * @return true 唯一 / false 不唯一
     */
    @GetMapping("/checkRoleNameUnique")
    @SaCheckPermission("system:role:view")
    public CommonResult<Boolean> checkRoleNameUnique(@RequestParam(value = "id", required = false) Long id,
                                                     @RequestParam("roleName") String roleName) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(roleName);
        return CommonResult.success(sysRoleService.checkRoleNameUnique(role));
    }

    /**
     * 校验角色编码是否唯一。
     *
     * @param id 角色ID，可为空
     * @param roleCode 角色编码
     * @return true 唯一 / false 不唯一
     */
    @GetMapping("/checkRoleCodeUnique")
    @SaCheckPermission("system:role:view")
    public CommonResult<Boolean> checkRoleCodeUnique(@RequestParam(value = "id", required = false) Long id,
                                                     @RequestParam("roleCode") String roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        return CommonResult.success(sysRoleService.checkRoleCodeUnique(role));
    }

    /**
     * 批量删除角色。
     *
     * @param batchDeletedDto 批量删除请求对象
     * @return 删除结果
     */
    @RepeatSubmit
    @PostMapping("/deleteBatch")
    @SaCheckPermission("system:role:delete")
    public CommonResult<Boolean> deleteBatch(
            @RequestBody @Valid SysRoleBatchDeletedDto batchDeletedDto) {
        sysRoleService.deleteBatch(batchDeletedDto);
        return CommonResult.success(true);
    }

    /**
     * 取消授权单个用户角色。
     *
     * @param authUserDto 请求对象
     * @return 处理结果
     */
    @RepeatSubmit
    @PostMapping("/cancelAuthUser")
    @SaCheckPermission("system:role:grant")
    public CommonResult<Boolean> cancelAuthUser(@RequestBody @Valid SysRoleAuthUserDto authUserDto) {
        sysRoleService.cancelAuthUser(authUserDto.getRoleId(), authUserDto.getUserId());
        return CommonResult.success(true);
    }

    /**
     * 批量取消授权用户角色。
     *
     * @param authUsersDto 请求对象
     * @return 处理结果
     */
    @RepeatSubmit
    @PostMapping("/cancelAuthUsers")
    @SaCheckPermission("system:role:grant")
    public CommonResult<Boolean> cancelAuthUsers(@RequestBody @Valid SysRoleAuthUsersDto authUsersDto) {
        sysRoleService.cancelAuthUsers(authUsersDto.getRoleId(), authUsersDto.getUserIds());
        return CommonResult.success(true);
    }

    /**
     * 批量选择授权用户角色。
     *
     * @param authUsersDto 请求对象
     * @return 处理结果
     */
    @RepeatSubmit
    @PostMapping("/authUsers")
    @SaCheckPermission("system:role:grant")
    public CommonResult<Boolean> authUsers(@RequestBody @Valid SysRoleAuthUsersDto authUsersDto) {
        sysRoleService.authUsers(authUsersDto.getRoleId(), authUsersDto.getUserIds());
        return CommonResult.success(true);
    }

    /**
     * 取消授权单个角色权限。
     *
     * @param authPermissionDto 请求对象
     * @return 处理结果
     */
    @RepeatSubmit
    @PostMapping("/cancelAuthPermission")
    @SaCheckPermission("system:role:grant")
    public CommonResult<Boolean> cancelAuthPermission(@RequestBody @Valid SysRoleAuthPermissionDto authPermissionDto) {
        sysRoleService.cancelAuthPermission(authPermissionDto.getRoleId(), authPermissionDto.getPermissionId());
        return CommonResult.success(true);
    }

    /**
     * 批量取消授权角色权限。
     *
     * @param authPermissionsDto 请求对象
     * @return 处理结果
     */
    @RepeatSubmit
    @PostMapping("/cancelAuthPermissions")
    @SaCheckPermission("system:role:grant")
    public CommonResult<Boolean> cancelAuthPermissions(@RequestBody @Valid SysRoleAuthPermissionsDto authPermissionsDto) {
        sysRoleService.cancelAuthPermissions(authPermissionsDto.getRoleId(), authPermissionsDto.getPermissionIds());
        return CommonResult.success(true);
    }

    /**
     * 批量选择授权角色权限。
     *
     * @param authPermissionsDto 请求对象
     * @return 处理结果
     */
    @RepeatSubmit
    @PostMapping("/authPermissions")
    @SaCheckPermission("system:role:grant")
    public CommonResult<Boolean> authPermissions(@RequestBody @Valid SysRoleAuthPermissionsDto authPermissionsDto) {
        sysRoleService.authPermissions(authPermissionsDto.getRoleId(), authPermissionsDto.getPermissionIds());
        return CommonResult.success(true);
    }
}
