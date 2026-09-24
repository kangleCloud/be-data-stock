package com.vita.system.associate.sysRoleDeptScope.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.associate.sysRoleDeptScope.vo
 * @Author znk
 * @CreateTime 2026-03-31 16:41:38
 * @Description: 角色自定义数据范围表 列表 VO
 * @version 1.0
 */
@Data
public class SysRoleDeptScopeListVo {
    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 部门ID
     */
    private Long deptId;

}
