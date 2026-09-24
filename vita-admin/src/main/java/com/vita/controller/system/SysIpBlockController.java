package com.vita.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.system.sysIpBlock.dto.SysIpBlockCreateDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockDeletedDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockSearchDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockUpdateDto;
import com.vita.system.sysIpBlock.service.ISysIpBlockService;
import com.vita.system.sysIpBlock.vo.SysIpBlockDetailVo;
import com.vita.system.sysIpBlock.vo.SysIpBlockListVo;
import com.vita.system.sysIpBlock.vo.SysIpBlockPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysIpBlock")
public class SysIpBlockController extends BaseController {

    /**
     * IP封禁表 服务。
     */
    private final ISysIpBlockService sysIpBlockService;

    public SysIpBlockController(ISysIpBlockService sysIpBlockService) {
        this.sysIpBlockService = sysIpBlockService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @RepeatSubmit
    @PostMapping("/add")
    @SaCheckPermission("system:ip-block:add")
    public CommonResult<Long> create(@RequestBody @Valid SysIpBlockCreateDto createDto) {
        return CommonResult.success(sysIpBlockService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @RepeatSubmit
    @PostMapping("/update")
    @SaCheckPermission("system:ip-block:update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysIpBlockUpdateDto updateDto) {
        sysIpBlockService.update(updateDto);
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
    @SaCheckPermission("system:ip-block:delete")
    public CommonResult<Boolean> delete(@RequestBody @Valid SysIpBlockDeletedDto deletedDto) {
        sysIpBlockService.delete(deletedDto);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    @SaCheckPermission("system:ip-block:view")
    public CommonResult<SysIpBlockDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysIpBlockService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    @SaCheckPermission("system:ip-block:view")
    public CommonResult<List<SysIpBlockListVo>> list(@Valid SysIpBlockSearchDto searchDto) {
        return CommonResult.success(sysIpBlockService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    @SaCheckPermission("system:ip-block:view")
    public CommonResult<PageResponse<SysIpBlockPageVo>> page(@Valid SysIpBlockSearchDto searchDto) {
        return CommonResult.success(sysIpBlockService.page(searchDto));
    }
}
