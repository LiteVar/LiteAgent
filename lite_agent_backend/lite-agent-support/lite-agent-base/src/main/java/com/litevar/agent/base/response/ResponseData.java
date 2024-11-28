package com.litevar.agent.base.response;

import lombok.Getter;

/**
 * http响应类
 *
 * @author uncle
 * @since 2024/7/3 15:28
 */
@Getter
public class ResponseData<T> {
    public static final String DEFAULT_SUCCESS_MESSAGE = "请求成功";

    public static final Integer DEFAULT_SUCCESS_CODE = 200;

    public static final Integer DEFAULT_ERROR_CODE = 500;

    /**
     * 状态码
     */
    private final Integer code;
    /**
     * 响应信息
     */
    private final String message;
    /**
     * 响应数据对象
     */
    private T data;

    private ResponseData(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseData<T> success(Integer code, String message, T data) {
        return new ResponseData<>(code, message, data);
    }

    public static ResponseData<String> success() {
        return success(DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, null);
    }

    public static <T> ResponseData<T> success(T data) {
        return success(DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static <T> ResponseData<T> error(Integer code, String message, T data) {
        return new ResponseData<>(code, message, data);
    }

    public static ResponseData<String> error(Integer code, String message) {
        return error(code, message, null);
    }

    public static ResponseData<String> error(Integer code) {
        return error(code, DEFAULT_SUCCESS_MESSAGE, null);
    }

}
