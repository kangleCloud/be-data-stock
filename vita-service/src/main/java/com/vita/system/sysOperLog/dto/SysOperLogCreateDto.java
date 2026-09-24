package com.vita.system.sysOperLog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysOperLog.dto
 * @Author znk
 * @CreateTime 2026-03-12 21:45:20
 * @Description: 系统操作日志表 创建 DTO
 */
@Data
public class SysOperLogCreateDto {
    /**
     * 模块
     */
    private String module;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String operUrl;

    /**
     * 操作IP
     */
    private String operIp;

    /**
     * 操作地点
     */
    private String operLocation;

    /**
     * 操作人名称
     */
    private String operName;

    /**
     * 操作人ID
     */
    private Long operUserId;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 请求参数
     */
    private String requestParam;

    /**
     * 返回结果
     */
    private String responseResult;

    /**
     * 状态 1成功 0失败
     */
    private Byte status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 耗时(ms)
     */
    private Long costTime;

    /**
     * 操作时间
     */
    private LocalDateTime operTime;

}
