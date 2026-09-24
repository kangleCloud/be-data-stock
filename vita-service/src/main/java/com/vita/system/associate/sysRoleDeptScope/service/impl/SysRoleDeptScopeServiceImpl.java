package com.vita.system.associate.sysRoleDeptScope.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeCreateDto;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeSearchDto;
import com.vita.system.associate.sysRoleDeptScope.dto.SysRoleDeptScopeUpdateDto;
import com.vita.system.associate.sysRoleDeptScope.entity.SysRoleDeptScope;
import com.vita.system.associate.sysRoleDeptScope.mapper.SysRoleDeptScopeMapper;
import com.vita.system.associate.sysRoleDeptScope.service.ISysRoleDeptScopeService;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopeDetailVo;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopeListVo;
import com.vita.system.associate.sysRoleDeptScope.vo.SysRoleDeptScopePageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleDeptScope.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:41:38
 * @Description: 角色自定义数据范围表 Service 实现
 * @version 1.0
 */
@Service
public class SysRoleDeptScopeServiceImpl extends ServiceImpl<SysRoleDeptScopeMapper, SysRoleDeptScope> implements ISysRoleDeptScopeService {

    @Resource
    private SysRoleDeptScopeMapper sysRoleDeptScopeMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysRoleDeptScopeCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRoleDeptScope data = new SysRoleDeptScope();
        BeanUtils.copyProperties(createDto, data);
        sysRoleDeptScopeMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysRoleDeptScopeUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysRoleDeptScope data = new SysRoleDeptScope();
        BeanUtils.copyProperties(updateDto, data);
        sysRoleDeptScopeMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getRequiredEntity(id);
        sysRoleDeptScopeMapper.deleteById(id);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysRoleDeptScopeDetailVo get(Long id) {
        SysRoleDeptScope data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysRoleDeptScopeDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysRoleDeptScopeListVo> list(SysRoleDeptScopeSearchDto searchDto) {
        List<SysRoleDeptScope> dataList = sysRoleDeptScopeMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysRoleDeptScopeListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysRoleDeptScopePageVo> page(SysRoleDeptScopeSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysRoleDeptScopeSearchDto();
        }
        PageResponse<SysRoleDeptScope> selectPage = sysRoleDeptScopeMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysRoleDeptScopePageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysRoleDeptScopePageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 根据角色ID列表删除角色自定义数据范围关联。
     *
     * @param roleIds 角色ID集合
     */
    @Override
    public void removeByRoleIds(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        remove(new LambdaQueryWrapperX<SysRoleDeptScope>().inIfPresent(SysRoleDeptScope::getRoleId, roleIds));
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysRoleDeptScope> buildQueryWrapper(SysRoleDeptScopeSearchDto searchDto) {
        LambdaQueryWrapperX<SysRoleDeptScope> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysRoleDeptScope::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysRoleDeptScope getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysRoleDeptScope data = sysRoleDeptScopeMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
