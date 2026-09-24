package com.vita.system.sysRole.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysRole.dto
 * @Author znk
 * @CreateTime 2026-03-31 14:45:57
 * @Description: 角色表 搜索 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleSearchDto extends PageRequest {

    /**
     * id
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

}
