package com.vita.system.sysIpBlock.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.dto
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 搜索 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysIpBlockSearchDto extends PageRequest {

    /**
     * IP地址
     */
    private String ip;

    /**
     * 状态 1启用 0禁用
     */
    private Byte status;

}
