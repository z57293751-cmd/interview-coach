package com.lujintao.coach.common;

import lombok.Data;

@Data
public class ApiResult<T> {

    private int code;
    private String msg;
    private T data;

    public ApiResult() {
    }

    public ApiResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data);
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(int code, String msg) {
        return new ApiResult<>(code, msg, null);
    }
}