package com.vita.system.sysMenu.dto;

import com.vita.core.page.PageRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysMenu.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:27:08
 * @Description: 菜单表 搜索 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenuSearchDto extends PageRequest {

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 类型 CONTENTS/MENU/LINK
     */
    private String menuType;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;
}
