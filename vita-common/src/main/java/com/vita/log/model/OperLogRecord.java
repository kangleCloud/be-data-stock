package com.vita.log.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.model
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: 操作日志记录模型
 * @Version: 1.0
 */
@Data
public class OperLogRecord {

    private String module;

    private String businessType;

    private String requestMethod;

    private String operUrl;

    private String operIp;

    private String operLocation;

    private String operName;

    private Long operUserId;

    private String className;

    private String methodName;

    private String requestParam;

    private String responseResult;

    private Byte status;

    private String errorMsg;

    private Long costTime;

    private LocalDateTime operTime;
}
