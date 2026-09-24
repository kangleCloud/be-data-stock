package com.vita.core.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.exception
 * @Author: znk
 * @CreateTime: 2026-03-04  21:16:39
 * @Description: 错误码
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
public class ErrorCode {

    private Integer code;

    private String msg;
}

