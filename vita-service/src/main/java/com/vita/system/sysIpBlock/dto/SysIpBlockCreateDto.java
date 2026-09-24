package com.vita.system.sysIpBlock.dto;

import lombok.Data;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.dto
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 创建 DTO
 * @version 1.0
 */
@Data
public class SysIpBlockCreateDto {
    /**
     * IP地址
     */
    private String ip;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

    /**
     * 备注
     */
    private String remark;

}
