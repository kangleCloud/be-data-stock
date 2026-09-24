package com.vita.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.core.CommonResult;
import com.vita.core.controller.BaseController;
import com.vita.core.page.PageResponse;
import com.vita.repeat.annotation.RepeatSubmit;
import com.vita.system.sysDept.dto.SysDeptCreateDto;
import com.vita.system.sysDept.dto.SysDeptDeletedDto;
import com.vita.system.sysDept.dto.SysDeptSearchDto;
import com.vita.system.sysDept.dto.SysDeptUpdateDto;
import com.vita.system.sysDept.service.ISysDeptService;
import com.vita.system.sysDept.vo.SysDeptDetailVo;
import com.vita.system.sysDept.vo.SysDeptListVo;
import com.vita.system.sysDept.vo.SysDeptPageVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.controller.system
 * @Author znk
 * @CreateTime 2026-03-31 16:29:38
 * @Description: 部门表 控制器
 * @version 1.0
 */
@Validated
@RestController
@RequestMapping("/system/sysDept")
public class SysDeptController extends BaseController {

    /**
     * 部门表 服务。
     */
    private final ISysDeptService sysDeptService;

    public SysDeptController(ISysDeptService sysDeptService) {
        this.sysDeptService = sysDeptService;
    }

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @RepeatSubmit
    @PostMapping("/add")
    @SaCheckPermission("system:dept:add")
    public CommonResult<Long> create(@RequestBody @Valid SysDeptCreateDto createDto) {
        return CommonResult.success(sysDeptService.create(createDto));
    }

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     * @return 更新结果
     */
    @RepeatSubmit
    @PostMapping("/update")
    @SaCheckPermission("system:dept:update")
    public CommonResult<Boolean> update(@RequestBody @Valid SysDeptUpdateDto updateDto) {
        sysDeptService.update(updateDto);
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
    @SaCheckPermission("system:dept:delete")
    public CommonResult<Boolean> delete(@RequestBody @Valid SysDeptDeletedDto deletedDto) {
        sysDeptService.delete(deletedDto);
        return CommonResult.success(true);
    }

    /**
     * 查询详情。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @GetMapping("/getDetail")
    @SaCheckPermission("system:dept:view")
    public CommonResult<SysDeptDetailVo> getDetail(@RequestParam("id") Long id) {
        return CommonResult.success(sysDeptService.get(id));
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表数据
     */
    @GetMapping("/list")
    @SaCheckPermission("system:dept:view")
    public CommonResult<List<SysDeptListVo>> list(@Valid SysDeptSearchDto searchDto) {
        return CommonResult.success(sysDeptService.list(searchDto));
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页数据
     */
    @GetMapping("/page")
    @SaCheckPermission("system:dept:view")
    public CommonResult<PageResponse<SysDeptPageVo>> page(@Valid SysDeptSearchDto searchDto) {
        return CommonResult.success(sysDeptService.page(searchDto));
    }
}
