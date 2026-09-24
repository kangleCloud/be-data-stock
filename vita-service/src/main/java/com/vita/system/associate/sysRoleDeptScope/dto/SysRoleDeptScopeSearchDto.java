package com.vita.system.associate.sysRoleDeptScope.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleDeptScope.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:41:38
 * @Description: 角色自定义数据范围表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleDeptScopeSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
