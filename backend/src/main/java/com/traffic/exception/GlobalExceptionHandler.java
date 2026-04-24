// src/main/java/com/traffic/exception/GlobalExceptionHandler.java
package com.traffic.exception;

import com.traffic.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        // ✅ 忽略视频流相关的响应已提交异常，避免 "No converter for ApiResponse with video/mp4"
        if (e instanceof IllegalStateException
                && e.getMessage() != null
                && e.getMessage().contains("getOutputStream")) {
            log.warn("视频流响应已提交，跳过全局异常包装: {}", e.getMessage());
            // 这里返回 null 让 Spring 继续处理，或者干脆不拦截
            return null;
        }

        log.error("系统异常", e);
        return ApiResponse.error(500, "系统错误: " + e.getMessage());
    }
}