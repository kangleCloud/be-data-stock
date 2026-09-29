package com.vita.auth.config;

import java.util.List;

/** 启动模块声明自身需要跳过登录拦截的精确路径。 */
public interface AuthExcludePathsProvider {
    List<String> paths();
}
