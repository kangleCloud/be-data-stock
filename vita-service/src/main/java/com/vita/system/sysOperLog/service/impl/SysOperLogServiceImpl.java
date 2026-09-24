package com.vita.system.sysOperLog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.sysOperLog.dto.SysOperLogCreateDto;
import com.vita.system.sysOperLog.dto.SysOperLogDeletedDto;
import com.vita.system.sysOperLog.dto.SysOperLogSearchDto;
import com.vita.system.sysOperLog.dto.SysOperLogUpdateDto;
import com.vita.system.sysOperLog.entity.SysOperLog;
import com.vita.system.sysOperLog.mapper.SysOperLogMapper;
import com.vita.system.sysOperLog.service.ISysOperLogService;
import com.vita.system.sysOperLog.vo.SysOperLogDetailVo;
import com.vita.system.sysOperLog.vo.SysOperLogListVo;
import com.vita.system.sysOperLog.vo.SysOperLogPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysOperLog.service.impl
 * @Author znk
 * @CreateTime 2026-03-12 21:45:20
 * @Description: 系统操作日志表 Service 实现
 */
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService {

    @Resource
    private SysOperLogMapper sysOperLogMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysOperLogCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysOperLog data = new SysOperLog();
        BeanUtils.copyProperties(createDto, data);
        sysOperLogMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysOperLogUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysOperLog data = new SysOperLog();
        BeanUtils.copyProperties(updateDto, data);
        sysOperLogMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(SysOperLogDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(deletedDto.getId());
        sysOperLogMapper.deleteById(deletedDto.getId());
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysOperLogDetailVo get(Long id) {
        SysOperLog data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysOperLogDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysOperLogListVo> list(SysOperLogSearchDto searchDto) {
        List<SysOperLog> dataList = sysOperLogMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysOperLogListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysOperLogPageVo> page(SysOperLogSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysOperLogSearchDto();
        }
        PageResponse<SysOperLog> selectPage = sysOperLogMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysOperLogPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysOperLogPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysOperLog> buildQueryWrapper(SysOperLogSearchDto searchDto) {
        LambdaQueryWrapperX<SysOperLog> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysOperLog::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysOperLog getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysOperLog data = sysOperLogMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
