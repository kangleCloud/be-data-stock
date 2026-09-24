package com.vita.log.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.model
 * @Author: znk
 * @CreateTime: 2026-03-26
 * @Description: 当前请求客户端信息快照
 * @Version: 1.0
 */
@Data
public class RequestClientInfo {

    /**
     * 客户端 IP。
     */
    private String ip;

    /**
     * 客户端归属地。
     */
    private String location;

    /**
     * 浏览器名称。
     */
    private String browser;

    /**
     * 操作系统名称。
     */
    private String os;

    /**
     * 当前请求解析时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;
}
