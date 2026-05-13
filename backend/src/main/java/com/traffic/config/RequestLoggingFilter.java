package com.traffic.config;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求日志过滤器
 * 在控制台输出精简的 API 请求信息，帮助开发调试
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String clientIp = getClientIp(request);

        String fullPath = query != null ? uri + "?" + query : uri;

        // 跳过静态资源和 WebSocket 的日志
        if (shouldSkip(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // 根据状态码选择日志级别
            String message = String.format("%-6s %3dms  %-3d  %s  %s",
                    method, duration, status, clientIp, fullPath);

            if (status >= 500) {
                log.error("[REQ] {}", message);
            } else if (status >= 400) {
                log.warn("[REQ] {}", message);
            } else {
                log.info("[REQ] {}", message);
            }
        }
    }

    /**
     * 跳过不需要记录的请求
     */
    private boolean shouldSkip(String uri) {
        return uri.startsWith("/ws/") ||           // WebSocket
                uri.startsWith("/traffic/video/") || // 视频流（大文件，日志太多）
                uri.endsWith(".js") ||
                uri.endsWith(".css") ||
                uri.endsWith(".png") ||
                uri.endsWith(".ico") ||
                uri.endsWith(".html");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // 只取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}