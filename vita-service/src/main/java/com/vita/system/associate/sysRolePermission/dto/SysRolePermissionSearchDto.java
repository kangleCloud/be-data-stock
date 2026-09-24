package com.vita.system.associate.sysRolePermission.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRolePermission.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:40:54
 * @Description: 角色权限关联表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRolePermissionSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
