package com.vita.system.sysUser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vita.core.enums.CommonStatusEnum;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.core.page.PageResponse;
import com.vita.crypto.Sm4Utils;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import com.vita.system.sysUser.dto.SysUserCreateDto;
import com.vita.system.sysUser.dto.SysUserDeletedDto;
import com.vita.system.sysUser.dto.SysUserSearchDto;
import com.vita.system.sysUser.dto.SysUserUpdateDto;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.mapper.SysUserMapper;
import com.vita.system.sysUser.service.ISysUserService;
import com.vita.system.sysUser.vo.SysUserDetailVo;
import com.vita.system.sysUser.vo.SysUserListVo;
import com.vita.system.sysUser.vo.SysUserPageVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.service.impl
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 Service 实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 新增数据并返回主键ID。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    @Override
    public Long create(SysUserCreateDto createDto) {
        if (createDto == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        validateSingleSuperAdmin(null, createDto.getIsSuperAdmin());
        SysUser data = new SysUser();
        BeanUtils.copyProperties(createDto, data);
        data.setPassword(Sm4Utils.encryptToBase64(data.getPassword()));
        sysUserMapper.insert(data);
        return data.getId();
    }

    /**
     * 更新指定数据。
     *
     * @param updateDto 更新请求对象
     */
    @Override
    public void update(SysUserUpdateDto updateDto) {
        if (updateDto == null || updateDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        getRequiredEntity(updateDto.getId());
        SysUser data = new SysUser();
        BeanUtils.copyProperties(updateDto, data);
        if (CharSequenceUtil.isBlank(updateDto.getPassword())) {
            // 空密码表示保持原密码，避免详情接口回传密码或前端重复提交密文。
            data.setPassword(null);
        } else {
            data.setPassword(Sm4Utils.encryptToBase64(updateDto.getPassword()));
            data.setPwdUpdateDate(LocalDateTime.now());
        }
        sysUserMapper.updateById(data);
    }

    /**
     * 删除指定数据。
     *
     * @param deletedDto 删除请求对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(SysUserDeletedDto deletedDto) {
        if (deletedDto == null || deletedDto.getId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysUser user = getRequiredEntity(deletedDto.getId());
        if (CommonStatusEnum.isEnabled(user.getIsSystem())
                || CommonStatusEnum.isEnabled(user.getIsSuperAdmin())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "系统内置或超级管理员用户不允许删除");
        }
        sysUserMapper.deleteById(deletedDto.getId());
    }

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    @Override
    public SysUserDetailVo get(Long id) {
        SysUser data = getRequiredEntity(id);
        return BeanUtil.copyProperties(data, SysUserDetailVo.class);
    }

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    @Override
    public List<SysUserListVo> list(SysUserSearchDto searchDto) {
        List<SysUser> dataList = sysUserMapper.selectList(buildQueryWrapper(searchDto));
        return BeanUtil.copyToList(dataList, SysUserListVo.class);
    }

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    @Override
    public PageResponse<SysUserPageVo> page(SysUserSearchDto searchDto) {
        if (searchDto == null) {
            searchDto = new SysUserSearchDto();
        }
        PageResponse<SysUser> selectPage = sysUserMapper.selectPage(searchDto, buildQueryWrapper(searchDto));
        PageResponse<SysUserPageVo> pageResponse = new PageResponse<>();
        pageResponse.setList(BeanUtil.copyToList(selectPage.getList(), SysUserPageVo.class));
        pageResponse.setTotal(selectPage.getTotal());
        return pageResponse;
    }

    /**
     * 按用户名查询用户。
     *
     * @param userName 用户名
     * @return 用户实体
     */
    @Override
    public SysUser getByUserName(String userName) {
        if (CharSequenceUtil.isBlank(userName)) {
            return null;
        }
        return sysUserMapper.selectOne(SysUser::getUserName, userName);
    }

    /**
     * 根据用户ID查询启用中的用户。
     *
     * @param userId 用户ID
     * @return 用户实体，不存在或未启用时返回 null
     */
    @Override
    public SysUser getActiveById(Long userId) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapperX<SysUser>()
                .eq(SysUser::getId, userId)
                .eq(SysUser::getStatus, CommonStatusEnum.ENABLED.getCode()));
    }

    /**
     * 更新登录信息。
     *
     * @param userId       用户ID
     * @param loginTime    登录时间
     * @param loginAddress 登录IP
     */
    @Override
    public void updateLoginInfo(Long userId, LocalDateTime loginTime, String loginAddress) {
        if (userId == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysUser updateData = new SysUser();
        updateData.setId(userId);
        updateData.setLoginTime(loginTime);
        updateData.setLoginAddress(loginAddress);
        sysUserMapper.updateById(updateData);
    }

    /**
     * 构造列表查询条件。
     *
     * @param searchDto 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapperX<SysUser> buildQueryWrapper(SysUserSearchDto searchDto) {
        LambdaQueryWrapperX<SysUser> queryWrapper = new LambdaQueryWrapperX<>();
        if (searchDto != null) {
            queryWrapper.eqIfPresent(SysUser::getDeptId, searchDto.getDeptId())
                    .likeIfPresent(SysUser::getUserName, searchDto.getUserName())
                    .likeIfPresent(SysUser::getNickName, searchDto.getNickName())
                    .likeIfPresent(SysUser::getMobile, searchDto.getMobile())
                    .eqIfPresent(SysUser::getStatus, searchDto.getStatus());
        }
        queryWrapper.orderByDesc(SysUser::getId);
        return queryWrapper;
    }

    /**
     * 查询并校验数据存在性。
     *
     * @param id 主键ID
     * @return 实体对象
     */
    private SysUser getRequiredEntity(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        SysUser data = sysUserMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return data;
    }

    /**
     * 超级管理员语义只保留在用户维度，并且全局只允许存在一个超级管理员用户。
     *
     * @param currentUserId 当前编辑用户ID，新增时为空
     * @param isSuperAdmin  目标超级管理员标记
     */
    private void validateSingleSuperAdmin(Long currentUserId, Byte isSuperAdmin) {
        if (!CommonStatusEnum.isEnabled(isSuperAdmin)) {
            return;
        }
        LambdaQueryWrapperX<SysUser> queryWrapper = new LambdaQueryWrapperX<SysUser>()
                .eq(SysUser::getIsSuperAdmin, CommonStatusEnum.ENABLED.getCode());
        if (currentUserId != null) {
            queryWrapper.ne(SysUser::getId, currentUserId);
        }
        if (sysUserMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "系统仅允许存在一个超级管理员用户");
        }
    }
}
