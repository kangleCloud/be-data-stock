package com.vita.system.sysIpBlock.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.auth.constant.AuthConstants;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.redis.RedisCache;
import com.vita.system.sysIpBlock.dto.SysIpBlockCreateDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockDeletedDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockSearchDto;
import com.vita.system.sysIpBlock.dto.SysIpBlockUpdateDto;
import com.vita.system.sysIpBlock.entity.SysIpBlock;
import com.vita.system.sysIpBlock.mapper.SysIpBlockMapper;
import com.vita.system.sysIpBlock.service.ISysIpBlockService;
import com.vita.system.sysIpBlock.vo.SysIpBlockDetailVo;
import com.vita.system.sysIpBlock.vo.SysIpBlockListVo;
import com.vita.system.sysIpBlock.vo.SysIpBlockPageVo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.service.impl
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 Service 实现
 */
@Service
public class SysIpBlockServiceImpl extends ServiceImpl<SysIpBlockMapper, SysIpBlock> implements ISysIpBlockService {

    @Resource
    private SysIpBlockMapper sysIpBlockMapper;

    @Resource
    private RedisCache redisCache;

    /**
     * 初始化封禁IP缓存
     */
    @PostConstruct
    public void initBlockedIpCache() {
        loadBlockedIpCache();
    }

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysIpBlockCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysIpBlock data = new SysIpBlock();
        BeanUtils.copyProperties(createDto, data);
        sysIpBlockMapper.insert(data);
        syncBlockCache(null, data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysIpBlockUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysIpBlock existed = getRequiredEntity(updateDto.getId());
        SysIpBlock data = new SysIpBlock();
        BeanUtils.copyProperties(updateDto, data);
        sysIpBlockMapper.updateById(data);
        data.setIp(StringUtils.hasText(data.getIp()) ? data.getIp() : existed.getIp());
        data.setStatus(data.getStatus() != null ? data.getStatus() : existed.getStatus());
        syncBlockCache(existed, data);
    }

    /**
     * 删除指定数据。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(SysIpBlockDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysIpBlock existed = getRequiredEntity(deletedDto.getId());
        sysIpBlockMapper.deleteById(deletedDto.getId());
        syncBlockCache(existed, null);
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysIpBlockDetailVo get(Long id) {
        SysIpBlock data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysIpBlockDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysIpBlockListVo> list(SysIpBlockSearchDto searchDto) {
        List<SysIpBlock> dataList = sysIpBlockMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysIpBlockListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysIpBlockPageVo> page(SysIpBlockSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysIpBlockSearchDto();
        }
        PageResponse<SysIpBlock> selectPage = sysIpBlockMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysIpBlockPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysIpBlockPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 判断指定 IP 是否被封禁。
     *
     * @param ip IP地址
     * @return true-已封禁
     */
    @Override
    public boolean isBlocked(String ip) {
        if (CharSequenceUtil.isBlank(ip)) {
            return false;
        }
        String cacheKey = AuthConstants.buildIpBlockKey(ip);
        Object cacheValue = redisCache.getCacheObject(cacheKey);
        if (cacheValue != null) {
            return true;
        }
        SysIpBlock sysIpBlock = sysIpBlockMapper.selectOne(new LambdaQueryWrapperX<SysIpBlock>()
                .eq(SysIpBlock::getIp, ip)
                .eq(SysIpBlock::getStatus, CommonStatusEnum.ENABLED.getCode()));
        if (sysIpBlock == null) {
            return false;
        }
        writeBlockCache(sysIpBlock);
        return true;
    }

    /**
     * 启动时装载启用的封禁IP到缓存。
     */
    @Override
    public void loadBlockedIpCache() {
        Collection<String> cacheKeys = redisCache.keys(AuthConstants.IP_BLOCK_KEY_PREFIX + "*");
        if (!cacheKeys.isEmpty()) {
            redisCache.deleteObject(cacheKeys);
        }
        List<SysIpBlock> blockedList = sysIpBlockMapper.selectList(new LambdaQueryWrapperX<SysIpBlock>()
                .eq(SysIpBlock::getStatus, CommonStatusEnum.ENABLED.getCode()));
        for (SysIpBlock sysIpBlock : blockedList) {
            writeBlockCache(sysIpBlock);
        }
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysIpBlock> buildQueryWrapper(SysIpBlockSearchDto searchDto) {
        LambdaQueryWrapperX<SysIpBlock> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null && CharSequenceUtil.isNotBlank(searchDto.getIp())) {
            queryWrapper.like(SysIpBlock::getIp, searchDto.getIp());
        } else if (searchDto != null) {
            queryWrapper.eqIfPresent(SysIpBlock::getStatus, searchDto.getStatus());
        }
        queryWrapper.orderByDesc(SysIpBlock::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysIpBlock getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysIpBlock data = sysIpBlockMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }

    /**
     * 同步封禁IP缓存
     *
     * @param oldData 历史数据
     * @param newData 新数据
     */
    private void syncBlockCache(SysIpBlock oldData, SysIpBlock newData) {
        if (oldData != null && StringUtils.hasText(oldData.getIp())) {
            redisCache.deleteObject(AuthConstants.buildIpBlockKey(oldData.getIp()));
        }
        if (newData != null && isEnabled(newData)) {
            writeBlockCache(newData);
        }
    }

    /**
     * 写入封禁IP缓存
     *
     * @param sysIpBlock IP封禁实体对象
     */
    private void writeBlockCache(SysIpBlock sysIpBlock) {
        if (sysIpBlock == null || !StringUtils.hasText(sysIpBlock.getIp()) || !isEnabled(sysIpBlock)) {
            return;
        }
        redisCache.setCacheObject(AuthConstants.buildIpBlockKey(sysIpBlock.getIp()), sysIpBlock.getId());
    }

    /**
     * 判断封禁IP记录是否启用
     *
     * @param sysIpBlock IP封禁实体对象
     * @return true-启用
     */
    private boolean isEnabled(SysIpBlock sysIpBlock) {
        return sysIpBlock != null && CommonStatusEnum.isEnabled(sysIpBlock.getStatus());
    }
}
