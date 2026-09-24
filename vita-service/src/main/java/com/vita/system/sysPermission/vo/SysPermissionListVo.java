package com.vita.system.sysPermission.vo;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysPermission.vo
 * @Author znk
 * @CreateTime 2026-03-31 16:27:59
 * @Description: 权限表 列表 VO
 * @version 1.0
 */
@Data
public class SysPermissionListVo {
    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限类型 MENU_ACTION/API/DATA/FIELD
     */
    private String permissionType;

    /**
     * 关联菜单ID，可为空
     */
    private Long menuId;

    /**
     * 接口方法 GET/POST
     */
    private String apiMethod;

    /**
     * 接口路径
     */
    private String apiPath;

    /**
     * Sa-Token鉴权标识，通常等于permission_code
     */
    private String authTag;

    /**
     * 排序
     */
    private Integer sortNo;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 是否系统内置 1是 0否
     */
    private Byte isSystem;

    /**
     * 备注
     */
    private String remark;

}
