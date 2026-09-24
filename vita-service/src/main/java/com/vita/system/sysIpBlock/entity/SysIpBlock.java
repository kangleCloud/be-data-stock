package com.vita.system.sysIpBlock.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;

import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysIpBlock.entity
 * @Author znk
 * @CreateTime 2026-03-21 00:23:39
 * @Description: IP封禁表 实体类
 * @version 1.0
 */
@Data
@TableName("sys_ip_block")
@EqualsAndHashCode(callSuper = true)
public class SysIpBlock extends BaseEntity {

    /**
     * IP地址
     */
    @TableField("ip")
    private String ip;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
