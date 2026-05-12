// src/main/java/com/traffic/exception/GlobalExceptionHandler.java
package com.traffic.exception;

import com.traffic.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, "参数错误: " + message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ApiResponse<Void> handleResourceAccess(ResourceAccessException e) {
        log.error("AI服务连接失败: {}", e.getMessage());
        return ApiResponse.error(503, "AI服务暂时不可用，请稍后重试");
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ApiResponse<Void> handleHttpClientError(HttpClientErrorException e) {
        log.error("AI服务客户端错误: status={}", e.getStatusCode());
        return ApiResponse.error(502, "AI服务请求失败");
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ApiResponse<Void> handleHttpServerError(HttpServerErrorException e) {
        log.error("AI服务内部错误: status={}", e.getStatusCode());
        return ApiResponse.error(502, "AI服务内部错误，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        if (e instanceof IllegalStateException
                && e.getMessage() != null
                && e.getMessage().contains("getOutputStream")) {
            log.warn("视频流响应已提交，跳过全局异常包装: {}", e.getMessage());
            return null;
        }

        log.error("系统异常", e);
        return ApiResponse.error(500, "系统错误: " + e.getMessage());
    }
}