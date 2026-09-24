package com.vita.system.associate.sysUserRole.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysUserRole.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:39:20
 * @Description: 用户角色关联表 搜索 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserRoleSearchDto extends PageRequest {

    /**
     * 用户ID
     */
    private Long userId;
}
