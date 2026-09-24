package com.vita.system.sysUser.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysUser.dto
 * @Author znk
 * @CreateTime 2026-03-06 21:44:32
 * @Description: 用户信息表 搜索 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserSearchDto extends PageRequest {

    /**
     * 部门ID
     */
    private Long deptId;


    /**
     * 用户名
     */
    private String userName;

    /**
     * 昵称
     */
    private String nickName;


    /**
     * 手机号
     */
    private String mobile;


    /**
     * 状态 1启用 0禁用
     */
    private Byte status;


}
