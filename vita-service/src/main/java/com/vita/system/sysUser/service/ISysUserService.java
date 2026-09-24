package com.vita.system.sysUser.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vita.core.page.PageResponse;
import com.vita.system.sysUser.dto.SysUserCreateDto;
import com.vita.system.sysUser.dto.SysUserDeletedDto;
import com.vita.system.sysUser.dto.SysUserSearchDto;
import com.vita.system.sysUser.dto.SysUserUpdateDto;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.vo.SysUserDetailVo;
import com.vita.system.sysUser.vo.SysUserListVo;
import com.vita.system.sysUser.vo.SysUserPageVo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.service
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 Service 接口
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 新增数据。
     *
     * @param createDto 创建请求对象
     * @return 新增记录ID
     */
    Long create(SysUserCreateDto createDto);

    /**
     * 更新数据。
     *
     * @param updateDto 更新请求对象
     */
    void update(SysUserUpdateDto updateDto);

    /**
     * 删除数据。
     *
     * @param deletedDto 删除请求对象
     */
    void delete(SysUserDeletedDto deletedDto);

    /**
     * 查询单条数据。
     *
     * @param id 主键ID
     * @return 详情对象
     */
    SysUserDetailVo get(Long id);

    /**
     * 列表查询。
     *
     * @param searchDto 查询条件
     * @return 列表VO
     */
    List<SysUserListVo> list(SysUserSearchDto searchDto);

    /**
     * 分页查询。
     *
     * @param searchDto 分页查询条件
     * @return 分页VO结果
     */
    PageResponse<SysUserPageVo> page(SysUserSearchDto searchDto);

    /**
     * 按用户名查询用户。
     *
     * @param userName 用户名
     * @return 用户实体
     */
    SysUser getByUserName(String userName);

    /**
     * 根据用户ID查询启用中的用户。
     *
     * @param userId 用户ID
     * @return 用户实体，不存在或未启用时返回 null
     */
    SysUser getActiveById(Long userId);

    /**
     * 更新登录信息。
     *
     * @param userId       用户ID
     * @param loginTime    登录时间
     * @param loginAddress 登录IP
     */
    void updateLoginInfo(Long userId, LocalDateTime loginTime, String loginAddress);
}
