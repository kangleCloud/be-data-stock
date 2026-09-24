package com.vita.core;

import com.vita.core.exception.ErrorCode;
import com.vita.core.exception.GlobalErrorCode;
import lombok.Data;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core
 * @Author: znk
 * @CreateTime: 2026-03-04  20:10:18
 * @Description: 统一返回结果封装类
 * @Version: 1.0
 */
@Data
public class CommonResult<T> implements Serializable {

    private static final long serialVersionUID = 5967655634352906571L;

    /**
     * 错误码
     *
     * @see ErrorCode#getCode()
     */
    private Integer code;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误提示，用户可阅读
     *
     * @see ErrorCode#getMsg() ()
     */
    private String msg;

    /**
     * 返回数据
     */
    private T content;

    /**
     * 将传入的 result 对象，转换成另外一个泛型结果的对象
     * <p>
     * 因为 A 方法返回的 CommonResult 对象，不满足调用其的 B 方法的返回，所以需要进行转换。
     *
     * @param result 传入的 result 对象
     * @param <T>    返回的泛型
     * @return 新的 CommonResult 对象
     */
    public static <T> CommonResult<T> error(CommonResult<?> result) {
        return error(result.getCode(), result.getMsg());
    }

    public static <T> CommonResult<T> error(Integer code, String message) {
        Assert.isTrue(!GlobalErrorCode.SUCCESS.getCode().equals(code), "code 必须是错误的！" );
        CommonResult<T> result = new CommonResult<>();
        result.code = code;
        result.success = false;
        result.msg = message;
        return result;
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }

    public static <T> CommonResult<T> error(String message) {
        CommonResult<T> result = new CommonResult<>();
        result.code = 200;
        result.success = false;
        result.msg = message;
        return result;
    }

    public static <T> CommonResult<T> success(T content) {
        CommonResult<T> result = new CommonResult<>();
        result.code = GlobalErrorCode.SUCCESS.getCode();
        result.success = true;
        result.content = content;
        result.msg = "接口调用成功";
        return result;
    }
}
