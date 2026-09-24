package com.vita.core.exception;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.exception
 * @Author: znk
 * @CreateTime: 2026-03-04  21:17:21
 * @Description: 全局错误码
 * @Version: 1.0
 */
public interface GlobalErrorCode {
    ErrorCode SUCCESS = new ErrorCode(200, "成功");

    // ========== 客户端错误段 ==========

    ErrorCode BAD_REQUEST = new ErrorCode(400, "请求参数不正确");
    ErrorCode UNAUTHORIZED = new ErrorCode(401, "账号未登录");
    ErrorCode FORBIDDEN = new ErrorCode(403, "没有该操作权限");
    ErrorCode NOT_FOUND = new ErrorCode(404, "请求未找到");
    ErrorCode METHOD_NOT_ALLOWED = new ErrorCode(405, "请求方法不正确");

    /**
     * 并发请求，不允许
     */
    ErrorCode LOCKED = new ErrorCode(423, "请求失败，请稍后重试");
    ErrorCode TOO_MANY_REQUESTS = new ErrorCode(429, "请求过于频繁，请稍后重试");

    // ========== 服务端错误段 ==========

    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode(500, "系统异常, 请联系管理员");
    ErrorCode SERVICE_UNAVAILABLE = new ErrorCode(503, "服务不可用");
    ErrorCode GATEWAY_TIMEOUT = new ErrorCode(504, "网关超时");

    // ========== 自定义错误段 ==========

    /**
     * 重复请求
     */
    ErrorCode REPEATED_REQUESTS = new ErrorCode(900, "重复请求，请稍后重试");
    ErrorCode DEMO_DENY = new ErrorCode(901, "演示模式，禁止写操作");

    ErrorCode UNKNOWN = new ErrorCode(999, "未知错误");

    /**
     * 业务异常
     */
    ErrorCode BUSINESS_EXCEPTION = new ErrorCode(1001, "业务异常，请联系管理员");
    ErrorCode BUILTIN_PARAM_CANNOT_MODIFY = new ErrorCode(1002, "内置参数不能修改，请联系管理员");

    // ========== 文件上传 业务错误段 2201-2299 ==========

    ErrorCode FILE_UPLOAD_EXPIRED = new ErrorCode(2201, "上传ID已失效");
    ErrorCode FILE_FORMAT_NOT_SUPPORTED = new ErrorCode(2202, "文件格式不支持");
    ErrorCode FILE_UPLOAD_FAILED = new ErrorCode(2203, "上传失败，请联系管理员");
    ErrorCode FILE_PART_SUFFIX_REQUIRED = new ErrorCode(2204, "分片上传时文件类型不能为空");
    ErrorCode FILE_PART_MIN_SIZE = new ErrorCode(2205, "分片文件大小必须大于最小限制");
    ErrorCode FILE_PART_NOT_FOUND = new ErrorCode(2206, "分片不存在");
    ErrorCode FILE_KEY_NOT_FOUND = new ErrorCode(2207, "文件不存在");
    ErrorCode FILE_MERGE_FAILED = new ErrorCode(2208, "文件合并失败");
    ErrorCode FILE_SIZE_EXCEEDS_LIMIT = new ErrorCode(2209, "文件大小超出限制");
    ErrorCode FILE_SIGN_SECRET_NOT_CONFIGURED = new ErrorCode(2210, "签名密钥未配置");
    ErrorCode FILE_PREFIX_DIR_TOO_LONG = new ErrorCode(2211, "前缀目录名称过长");
    ErrorCode FILE_PREFIX_DIR_NOT_SUPPORT_MULTI_LEVEL = new ErrorCode(2212, "前缀目录不支持多级");
    ErrorCode FILE_SIGNATURE_INVALID = new ErrorCode(2213, "签名校验失败");
}
