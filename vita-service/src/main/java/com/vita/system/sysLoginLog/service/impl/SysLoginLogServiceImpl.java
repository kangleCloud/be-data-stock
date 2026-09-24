package com.vita.system.sysLoginLog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.sysLoginLog.dto.SysLoginLogCreateDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogDeletedDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogSearchDto;
import com.vita.system.sysLoginLog.dto.SysLoginLogUpdateDto;
import com.vita.system.sysLoginLog.entity.SysLoginLog;
import com.vita.system.sysLoginLog.mapper.SysLoginLogMapper;
import com.vita.system.sysLoginLog.service.ISysLoginLogService;
import com.vita.system.sysLoginLog.vo.SysLoginLogDetailVo;
import com.vita.system.sysLoginLog.vo.SysLoginLogListVo;
import com.vita.system.sysLoginLog.vo.SysLoginLogPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysLoginLog.service.impl
 * @Author znk
 * @CreateTime 2026-03-21 00:20:48
 * @Description: 系统访问记录 Service 实现
 * @version 1.0
 */
@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements ISysLoginLogService {

    @Resource
    private SysLoginLogMapper sysLoginLogMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysLoginLogCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysLoginLog data = new SysLoginLog();
        BeanUtils.copyProperties(createDto, data);
        sysLoginLogMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysLoginLogUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysLoginLog data = new SysLoginLog();
        BeanUtils.copyProperties(updateDto, data);
        sysLoginLogMapper.updateById(data);
    }

    /**
     * 仅更新登录地点，供异步归属地补全链路使用。
     *
     * @param id 主键ID
     * @param loginLocation 登录地点
     */
    @Override
    public void updateLoginLocation(Long id, String loginLocation) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysLoginLog data = new SysLoginLog();
        data.setId(id);
        data.setLoginLocation(loginLocation);
        sysLoginLogMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(SysLoginLogDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(deletedDto.getId());
        sysLoginLogMapper.deleteById(deletedDto.getId());
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysLoginLogDetailVo get(Long id) {
        SysLoginLog data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysLoginLogDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysLoginLogListVo> list(SysLoginLogSearchDto searchDto) {
        List<SysLoginLog> dataList = sysLoginLogMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysLoginLogListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysLoginLogPageVo> page(SysLoginLogSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysLoginLogSearchDto();
        }
        PageResponse<SysLoginLog> selectPage = sysLoginLogMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysLoginLogPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysLoginLogPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysLoginLog> buildQueryWrapper(SysLoginLogSearchDto searchDto) {
        LambdaQueryWrapperX<SysLoginLog> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && StrUtil.isNotBlank(searchDto.getKeyword())) {
            // TODO 根据业务字段补充关键字查询条件
        }
        queryWrapper.orderByDesc(SysLoginLog::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysLoginLog getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysLoginLog data = sysLoginLogMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }
}
