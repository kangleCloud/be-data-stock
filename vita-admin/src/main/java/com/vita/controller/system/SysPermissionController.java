package com.vita.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.system.sysPermission.dto.*;
import com.vita.system.sysPermission.entity.SysPermission;
import com.vita.system.sysPermission.service.ISysPermissionService;
import com.vita.system.sysPermission.vo.SysPermissionDetailVo;
import com.vita.system.sysPermission.vo.SysPermissionListVo;
import com.vita.system.sysPermission.vo.SysPermissionOptionVo;
import com.vita.system.sysPermission.vo.SysPermissionPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysPermission")
public class SysPermissionController extends BaseController {

    /**
     * 权限表 服务。
     */
    private final ISysPermissionService sysPermissionService;

    public SysPermissionController(ISysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @RepeatSubmit
    @PostMapping("/add")
    @SaCheckPermission("system:permission:add")
    public CommonResult<Long> create(@RequestBody @Valid SysPermissionCreateDto createDto) {
        return CommonResult.success(sysPermissionService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @RepeatSubmit
    @PostMapping("/update")
    @SaCheckPermission("system:permission:update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysPermissionUpdateDto updateDto) {
        sysPermissionService.update(updateDto);
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
    @SaCheckPermission("system:permission:delete")
    public CommonResult<Boolean> delete(@RequestBody @Valid SysPermissionDeletedDto deletedDto) {
        sysPermissionService.delete(deletedDto);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    @SaCheckPermission("system:permission:view")
    public CommonResult<SysPermissionDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysPermissionService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    @SaCheckPermission("system:permission:view")
    public CommonResult<List<SysPermissionListVo>> list(@Valid SysPermissionSearchDto searchDto) {
        return CommonResult.success(sysPermissionService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    @SaCheckPermission("system:permission:view")
    public CommonResult<PageResponse<SysPermissionPageVo>> page(@Valid SysPermissionSearchDto searchDto) {
        return CommonResult.success(sysPermissionService.page(searchDto));
    }

    /**
     * 查询所有权限。
     *
     * @return 权限列表
     */
    @GetMapping("/getAll")
    @SaCheckPermission("system:permission:view")
    public CommonResult<List<SysPermissionListVo>> getAll() {
        return CommonResult.success(sysPermissionService.getAll());
    }

    /**
     * 根据角色ID查询权限。
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @GetMapping("/getByRoleId")
    @SaCheckPermission("system:permission:view")
    public CommonResult<List<SysPermissionListVo>> getByRoleId(@RequestParam("roleId") Long roleId) {
        return CommonResult.success(sysPermissionService.getListByRoleId(roleId));
    }

    /**
     * 根据角色ID获取权限选择框列表。
     *
     * @param roleId 角色ID
     * @return 权限选择框列表
     */
    @GetMapping("/getOptionListByRoleId")
    @SaCheckPermission("system:permission:view")
    public CommonResult<List<SysPermissionOptionVo>> getOptionListByRoleId(@RequestParam("roleId") Long roleId) {
        return CommonResult.success(sysPermissionService.getOptionListByRoleId(roleId));
    }

    /**
     * 校验权限编码是否唯一。
     *
     * @param id 权限ID，可为空
     * @param permissionCode 权限编码
     * @return true 唯一 / false 不唯一
     */
    @GetMapping("/checkPermissionCodeUnique")
    @SaCheckPermission("system:permission:view")
    public CommonResult<Boolean> checkPermissionCodeUnique(@RequestParam(value = "id", required = false) Long id,
                                                           @RequestParam("permissionCode") String permissionCode) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermissionCode(permissionCode);
        return CommonResult.success(sysPermissionService.checkPermissionCodeUnique(permission));
    }

    /**
     * 校验鉴权标识是否唯一。
     *
     * @param id 权限ID，可为空
     * @param authTag 鉴权标识
     * @return true 唯一 / false 不唯一
     */
    @GetMapping("/checkAuthTagUnique")
    @SaCheckPermission("system:permission:view")
    public CommonResult<Boolean> checkAuthTagUnique(@RequestParam(value = "id", required = false) Long id,
                                                    @RequestParam("authTag") String authTag) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setAuthTag(authTag);
        return CommonResult.success(sysPermissionService.checkAuthTagUnique(permission));
    }

    /**
     * 批量删除权限。
     *
     * @param batchDeletedDto 批量删除请求对象
     * @return 删除结果
     */
    @RepeatSubmit
    @PostMapping("/deleteBatch")
    @SaCheckPermission("system:permission:delete")
    public CommonResult<Boolean> deleteBatch(
            @RequestBody @Valid SysPermissionBatchDeletedDto batchDeletedDto) {
        sysPermissionService.deleteBatch(batchDeletedDto);
        return CommonResult.success(true);
    }
}
