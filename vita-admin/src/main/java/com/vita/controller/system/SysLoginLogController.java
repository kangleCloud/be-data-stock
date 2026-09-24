package com.vita.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.system.sysLoginLog.dto.SysLoginLogDeletedDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogSearchDto;
import com.vita.system.sysLoginLog.service.ISysLoginLogService;
import com.vita.system.sysLoginLog.vo.SysLoginLogDetailVo;
import com.vita.system.sysLoginLog.vo.SysLoginLogListVo;
import com.vita.system.sysLoginLog.vo.SysLoginLogPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-21 00:20:48
 * @Description: 系统访问记录 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysLoginLog")
public class SysLoginLogController extends BaseController {

    /**
     * 系统访问记录 服务。
     */
    private final ISysLoginLogService sysLoginLogService;

    public SysLoginLogController(ISysLoginLogService sysLoginLogService) {
        this.sysLoginLogService = sysLoginLogService;
    }

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     * @return 删除结果
     */
    @RepeatSubmit
    @PostMapping("/delete")
    @SaCheckPermission("system:login-log:delete")
    public CommonResult<Boolean> delete(@RequestBody @Valid SysLoginLogDeletedDto deletedDto) {
        sysLoginLogService.delete(deletedDto);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    @SaCheckPermission("system:login-log:view")
    public CommonResult<SysLoginLogDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysLoginLogService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    @SaCheckPermission("system:login-log:view")
    public CommonResult<List<SysLoginLogListVo>> list(@Valid SysLoginLogSearchDto searchDto) {
        return CommonResult.success(sysLoginLogService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    @SaCheckPermission("system:login-log:view")
    public CommonResult<PageResponse<SysLoginLogPageVo>> page(@Valid SysLoginLogSearchDto searchDto) {
        return CommonResult.success(sysLoginLogService.page(searchDto));
    }
}
