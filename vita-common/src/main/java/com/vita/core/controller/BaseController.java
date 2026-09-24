package com.vita.core.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.controller
 * @Author: znk
 * @CreateTime: 2026-03-04  14:29:32
 * @Description: 基础控制器类
 * @Version: 1.0
 */
@RestController
public class BaseController {

    // 日志对象，子类可以直接使用
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
}
