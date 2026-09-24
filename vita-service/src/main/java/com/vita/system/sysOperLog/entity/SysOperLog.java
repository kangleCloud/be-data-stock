package com.vita.system.sysOperLog.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vita.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysOperLog.entity
 * @Author znk
 * @CreateTime 2026-03-12 21:45:20
 * @Description: 系统操作日志表 实体类
 * @version 1.0
 */
@Data
@TableName("sys_oper_log")
@EqualsAndHashCode(callSuper = true)
public class SysOperLog extends BaseEntity {

    /**
     * 模块
     */
    @TableField("module")
    private String module;

    /**
     * 业务类型
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 请求方式
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求URL
     */
    @TableField("oper_url")
    private String operUrl;

    /**
     * 操作IP
     */
    @TableField("oper_ip")
    private String operIp;

    /**
     * 操作地点
     */
    @TableField("oper_location")
    private String operLocation;

    /**
     * 操作人名称
     */
    @TableField("oper_name")
    private String operName;

    /**
     * 操作人ID
     */
    @TableField("oper_user_id")
    private Long operUserId;

    /**
     * 类名
     */
    @TableField("class_name")
    private String className;

    /**
     * 方法名
     */
    @TableField("method_name")
    private String methodName;

    /**
     * 请求参数
     */
    @TableField("request_param")
    private String requestParam;

    /**
     * 返回结果
     */
    @TableField("response_result")
    private String responseResult;

    /**
     * 状态 1成功 0失败
     */
    @TableField("status")
    private Byte status;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 耗时(ms)
     */
    @TableField("cost_time")
    private Long costTime;

    /**
     * 操作时间
     */
    @TableField("oper_time")
    private LocalDateTime operTime;
}
