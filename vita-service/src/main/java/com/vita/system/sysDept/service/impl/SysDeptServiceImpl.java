package com.vita.system.sysDept.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.sysDept.dto.SysDeptCreateDto;
import com.vita.system.sysDept.dto.SysDeptDeletedDto;
import com.vita.system.sysDept.dto.SysDeptSearchDto;
import com.vita.system.sysDept.dto.SysDeptUpdateDto;
import com.vita.system.sysDept.entity.SysDept;
import com.vita.system.sysDept.mapper.SysDeptMapper;
import com.vita.system.sysDept.service.ISysDeptService;
import com.vita.system.sysDept.vo.SysDeptDetailVo;
import com.vita.system.sysDept.vo.SysDeptListVo;
import com.vita.system.sysDept.vo.SysDeptPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysDept.service.impl
 * @Author znk
 * @CreateTime 2026-03-31 16:29:38
 * @Description: 部门表 Service 实现
 * @version 1.0
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

    @Resource
    private SysDeptMapper sysDeptMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysDeptCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysDept data = new SysDept();
        BeanUtils.copyProperties(createDto, data);
        sysDeptMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysDeptUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysDept data = new SysDept();
        BeanUtils.copyProperties(updateDto, data);
        sysDeptMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(SysDeptDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(deletedDto.getId());
        sysDeptMapper.deleteById(deletedDto.getId());
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysDeptDetailVo get(Long id) {
        SysDept data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysDeptDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysDeptListVo> list(SysDeptSearchDto searchDto) {
        List<SysDept> dataList = sysDeptMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysDeptListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysDeptPageVo> page(SysDeptSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysDeptSearchDto();
        }
        PageResponse<SysDept> selectPage = sysDeptMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysDeptPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysDeptPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysDept> buildQueryWrapper(SysDeptSearchDto searchDto) {
        LambdaQueryWrapperX<SysDept> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysDept::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysDept getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysDept data = sysDeptMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
