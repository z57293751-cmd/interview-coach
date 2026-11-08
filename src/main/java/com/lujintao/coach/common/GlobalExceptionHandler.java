package com.lujintao.coach.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AiServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResult<Void> handleAi(AiServiceException e) {
        return ApiResult.fail(503, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBadRequest(IllegalArgumentException e) {
        return ApiResult.fail(400, e.getMessage());
    }

    /** 访问了不存在的路径：给个 404 和明确提示，而不是 500「服务器开小差」 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleNotFound(NoResourceFoundException e) {
        return ApiResult.fail(404, "没有这个地址：" + e.getResourcePath() + "。可以试试 / 或 /question.html 或 /mock.html");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleOther(Exception e) {
        return ApiResult.fail(500, "服务器开小差了：" + e.getMessage());
    }
}