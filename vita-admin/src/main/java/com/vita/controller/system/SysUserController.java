package com.vita.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.log.annotation.Log;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.system.sysUser.dto.SysUserCreateDto;
import com.vita.system.sysUser.dto.SysUserDeletedDto;
import com.vita.system.sysUser.dto.SysUserSearchDto;
import com.vita.system.sysUser.dto.SysUserUpdateDto;
import com.vita.system.sysUser.service.ISysUserService;
import com.vita.system.sysUser.vo.SysUserDetailVo;
import com.vita.system.sysUser.vo.SysUserListVo;
import com.vita.system.sysUser.vo.SysUserPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 控制器
 */
@Validated
@RestController
@RequestMapping("/system/sysUser" )
public class SysUserController extends BaseController {

    /**
     * 用户信息表 服务。
     */
    private final ISysUserService sysUserService;

    public SysUserController(ISysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Log(module = "系统用户", businessType = "CREATE", saveResponseData = false)
    @RepeatSubmit
    @PostMapping("/add" )
    @SaCheckPermission("system:user:add")
    public CommonResult<Long> create(@RequestBody @Valid SysUserCreateDto createDto) {
        return CommonResult.success(sysUserService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @Log(module = "admin", businessType = "UPDATE", saveResponseData = false)
    @RepeatSubmit
    @PostMapping("/update" )
    @SaCheckPermission("system:user:update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysUserUpdateDto updateDto) {
        sysUserService.update(updateDto);
        return CommonResult.success(true);
    }

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     * @return 删除结果
     */
    @Log(module = "admin", businessType = "DELETE", saveResponseData = false)
    @RepeatSubmit
    @PostMapping("/delete")
    @SaCheckPermission("system:user:delete")
    public CommonResult<Boolean> delete(@RequestBody @Valid SysUserDeletedDto deletedDto) {
        sysUserService.delete(deletedDto);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail" )
    @SaCheckPermission("system:user:view")
    public CommonResult<SysUserDetailVo> getDetail(@RequestParam("id" ) Long id) {
        return CommonResult.success(sysUserService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list" )
    @SaCheckPermission("system:user:view")
    public CommonResult<List<SysUserListVo>> list(@Valid SysUserSearchDto searchDto) {
        return CommonResult.success(sysUserService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page" )
    @SaCheckPermission("system:user:view")
    public CommonResult<PageResponse<SysUserPageVo>> page(@Valid SysUserSearchDto searchDto) {
        return CommonResult.success(sysUserService.page(searchDto));
    }
}
